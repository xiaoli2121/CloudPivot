package io.cloudpivot.iam.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.auth.service.AuthService;
import io.cloudpivot.auth.service.PasswordService;
import io.cloudpivot.auth.service.RedisService;
import io.cloudpivot.common.api.BadRequestException;
import io.cloudpivot.common.api.NotFoundException;
import io.cloudpivot.iam.persistence.entity.IamMenuEntity;
import io.cloudpivot.iam.persistence.entity.IamOrgEntity;
import io.cloudpivot.iam.persistence.entity.IamRoleDataScopeRelEntity;
import io.cloudpivot.iam.persistence.entity.IamRoleEntity;
import io.cloudpivot.iam.persistence.entity.IamRolePermissionRelEntity;
import io.cloudpivot.iam.persistence.entity.IamUserEntity;
import io.cloudpivot.iam.persistence.entity.IamUserRoleRelEntity;
import io.cloudpivot.iam.persistence.mapper.IamMenuMapper;
import io.cloudpivot.iam.persistence.mapper.IamOrgMapper;
import io.cloudpivot.iam.persistence.mapper.IamRoleDataScopeRelMapper;
import io.cloudpivot.iam.persistence.mapper.IamRoleMapper;
import io.cloudpivot.iam.persistence.mapper.IamRolePermissionRelMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserRoleRelMapper;

@Service
public class IamCommandService {

    private final IamUserMapper iamUserMapper;
    private final IamOrgMapper iamOrgMapper;
    private final IamRoleMapper iamRoleMapper;
    private final IamUserRoleRelMapper iamUserRoleRelMapper;
    private final IamRolePermissionRelMapper iamRolePermissionRelMapper;
    private final IamRoleDataScopeRelMapper iamRoleDataScopeRelMapper;
    private final IamMenuMapper iamMenuMapper;
    private final PasswordService passwordService;
    private final AuthService authService;
    private final RedisService redisService;

    public IamCommandService(
            IamUserMapper iamUserMapper,
            IamOrgMapper iamOrgMapper,
            IamRoleMapper iamRoleMapper,
            IamUserRoleRelMapper iamUserRoleRelMapper,
            IamRolePermissionRelMapper iamRolePermissionRelMapper,
            IamRoleDataScopeRelMapper iamRoleDataScopeRelMapper,
            IamMenuMapper iamMenuMapper,
            PasswordService passwordService,
            AuthService authService,
            RedisService redisService) {
        this.iamUserMapper = iamUserMapper;
        this.iamOrgMapper = iamOrgMapper;
        this.iamRoleMapper = iamRoleMapper;
        this.iamUserRoleRelMapper = iamUserRoleRelMapper;
        this.iamRolePermissionRelMapper = iamRolePermissionRelMapper;
        this.iamRoleDataScopeRelMapper = iamRoleDataScopeRelMapper;
        this.iamMenuMapper = iamMenuMapper;
        this.passwordService = passwordService;
        this.authService = authService;
        this.redisService = redisService;
    }

    @Transactional
    public Map<String, Object> createUser(Map<String, Object> request, UserPrincipal actor) {
        String loginName = requiredString(request, "loginName");
        assertUniqueUserLoginName(loginName, null);
        IamOrgEntity org = requiredOrg(requiredLong(request, "orgId"));

        IamUserEntity entity = new IamUserEntity();
        applyCreateAudit(entity, actor.userId());
        entity.setUserName(requiredString(request, "userName"));
        entity.setLoginName(loginName);
        entity.setPasswordHash(passwordService.encode(requiredString(request, "password")));
        entity.setOrgId(org.getId());
        entity.setPhone(optionalString(request, "phone"));
        entity.setEmail(optionalString(request, "email"));
        entity.setUserStatus(defaultString(request, "userStatus", "ENABLED"));
        entity.setSuperAdminFlag(0);
        entity.setAuthVersion(0L);
        iamUserMapper.insert(entity);
        return userResponse(entity, org, List.of());
    }

    @Transactional
    public Map<String, Object> updateUser(Long userId, Map<String, Object> request, UserPrincipal actor) {
        IamUserEntity user = requiredUser(userId);
        IamOrgEntity org = requiredOrg(requiredLong(request, "orgId"));
        LocalDateTime now = LocalDateTime.now();
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, userId)
                        .eq(IamUserEntity::getDeletedFlag, 0)
                        .set(IamUserEntity::getUserName, requiredString(request, "userName"))
                        .set(IamUserEntity::getOrgId, org.getId())
                        .set(IamUserEntity::getPhone, optionalString(request, "phone"))
                        .set(IamUserEntity::getEmail, optionalString(request, "email"))
                        .set(IamUserEntity::getUserStatus, defaultString(request, "userStatus", user.getUserStatus()))
                        .set(IamUserEntity::getUpdatedBy, actor.userId())
                        .set(IamUserEntity::getUpdatedTime, now));
        user.setUserName(requiredString(request, "userName"));
        user.setOrgId(org.getId());
        user.setPhone(optionalString(request, "phone"));
        user.setEmail(optionalString(request, "email"));
        user.setUserStatus(defaultString(request, "userStatus", user.getUserStatus()));
        authService.evictUserAuthCache(userId);
        return userResponse(user, org, roleCodes(userId));
    }

    @Transactional
    public void deleteUser(Long userId, UserPrincipal actor) {
        IamUserEntity user = requiredUser(userId);
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, userId)
                        .set(IamUserEntity::getDeletedFlag, 1)
                        .set(IamUserEntity::getAuthVersion, authVersion(user) + 1)
                        .set(IamUserEntity::getUpdatedBy, actor.userId())
                        .set(IamUserEntity::getUpdatedTime, LocalDateTime.now()));
        authService.revokeUserSessions(userId);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword, UserPrincipal actor) {
        IamUserEntity user = requiredUser(userId);
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, userId)
                        .set(IamUserEntity::getPasswordHash, passwordService.encode(newPassword))
                        .set(IamUserEntity::getAuthVersion, authVersion(user) + 1)
                        .set(IamUserEntity::getUpdatedBy, actor.userId())
                        .set(IamUserEntity::getUpdatedTime, LocalDateTime.now()));
        authService.revokeUserSessions(userId);
    }

    @Transactional
    public Map<String, Object> updateUserStatus(Long userId, String userStatus, UserPrincipal actor) {
        IamUserEntity user = requiredUser(userId);
        long nextAuthVersion = authVersion(user) + 1;
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, userId)
                        .set(IamUserEntity::getUserStatus, userStatus)
                        .set(IamUserEntity::getAuthVersion, nextAuthVersion)
                        .set(IamUserEntity::getUpdatedBy, actor.userId())
                        .set(IamUserEntity::getUpdatedTime, LocalDateTime.now()));
        authService.revokeUserSessions(userId);
        user.setUserStatus(userStatus);
        user.setAuthVersion(nextAuthVersion);
        return userResponse(user, requiredOrg(user.getOrgId()), roleCodes(userId));
    }

    @Transactional
    public void unlockUser(Long userId, UserPrincipal actor) {
        requiredUser(userId);
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, userId)
                        .set(IamUserEntity::getLockExpireTime, null)
                        .set(IamUserEntity::getUpdatedBy, actor.userId())
                        .set(IamUserEntity::getUpdatedTime, LocalDateTime.now()));
    }

    @Transactional
    public Map<String, Object> assignUserRoles(Long userId, List<Long> roleIds, UserPrincipal actor) {
        requiredUser(userId);
        iamUserRoleRelMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserRoleRelEntity>()
                        .eq(IamUserRoleRelEntity::getUserId, userId)
                        .eq(IamUserRoleRelEntity::getDeletedFlag, 0)
                        .set(IamUserRoleRelEntity::getDeletedFlag, 1)
                        .set(IamUserRoleRelEntity::getUpdatedBy, actor.userId())
                        .set(IamUserRoleRelEntity::getUpdatedTime, LocalDateTime.now()));

        for (Long roleId : roleIds) {
            requiredRole(roleId);
            IamUserRoleRelEntity rel = new IamUserRoleRelEntity();
            applyCreateAudit(rel, actor.userId());
            rel.setUserId(userId);
            rel.setRoleId(roleId);
            iamUserRoleRelMapper.insert(rel);
        }
        authService.evictUserAuthCache(userId);

        return Map.of(
                "userId", userId,
                "roles", roleCodes(userId));
    }

    @Transactional
    public void forceLogout(Long userId) {
        requiredUser(userId);
        authService.revokeUserSessions(userId);
    }

    @Transactional
    public Map<String, Object> createRole(Map<String, Object> request, UserPrincipal actor) {
        String roleCode = requiredString(request, "roleCode");
        assertUniqueRoleCode(roleCode, null);
        IamRoleEntity role = new IamRoleEntity();
        applyCreateAudit(role, actor.userId());
        role.setRoleCode(roleCode);
        role.setRoleName(requiredString(request, "roleName"));
        role.setDataScope(defaultString(request, "dataScope", "SELF"));
        iamRoleMapper.insert(role);
        return roleResponse(role, List.of());
    }

    @Transactional
    public Map<String, Object> updateRole(Long roleId, Map<String, Object> request, UserPrincipal actor) {
        IamRoleEntity role = requiredRole(roleId);
        iamRoleMapper.update(
                null,
                new LambdaUpdateWrapper<IamRoleEntity>()
                        .eq(IamRoleEntity::getId, roleId)
                        .set(IamRoleEntity::getRoleName, requiredString(request, "roleName"))
                        .set(IamRoleEntity::getDataScope, defaultString(request, "dataScope", role.getDataScope()))
                        .set(IamRoleEntity::getUpdatedBy, actor.userId())
                        .set(IamRoleEntity::getUpdatedTime, LocalDateTime.now()));
        role.setRoleName(requiredString(request, "roleName"));
        role.setDataScope(defaultString(request, "dataScope", role.getDataScope()));
        return roleResponse(role, permissionCodes(roleId));
    }

    @Transactional
    public Map<String, Object> assignRolePermissions(Long roleId, List<String> permissionCodes, UserPrincipal actor) {
        requiredRole(roleId);
        iamRolePermissionRelMapper.update(
                null,
                new LambdaUpdateWrapper<IamRolePermissionRelEntity>()
                        .eq(IamRolePermissionRelEntity::getRoleId, roleId)
                        .eq(IamRolePermissionRelEntity::getDeletedFlag, 0)
                        .set(IamRolePermissionRelEntity::getDeletedFlag, 1)
                        .set(IamRolePermissionRelEntity::getUpdatedBy, actor.userId())
                        .set(IamRolePermissionRelEntity::getUpdatedTime, LocalDateTime.now()));

        for (String permissionCode : permissionCodes) {
            IamRolePermissionRelEntity rel = new IamRolePermissionRelEntity();
            applyCreateAudit(rel, actor.userId());
            rel.setRoleId(roleId);
            rel.setResourceType(resourceType(permissionCode));
            rel.setPermissionCode(permissionCode);
            iamRolePermissionRelMapper.insert(rel);
        }
        authService.evictUsersByRoleId(roleId);

        return Map.of(
                "roleId", roleId,
                "permissionCodes", permissionCodes(roleId));
    }

    @Transactional
    public Map<String, Object> assignRoleDataScope(Long roleId, String dataScope, List<Long> orgIds, UserPrincipal actor) {
        IamRoleEntity role = requiredRole(roleId);
        iamRoleMapper.update(
                null,
                new LambdaUpdateWrapper<IamRoleEntity>()
                        .eq(IamRoleEntity::getId, roleId)
                        .set(IamRoleEntity::getDataScope, dataScope)
                        .set(IamRoleEntity::getUpdatedBy, actor.userId())
                        .set(IamRoleEntity::getUpdatedTime, LocalDateTime.now()));

        iamRoleDataScopeRelMapper.update(
                null,
                new LambdaUpdateWrapper<IamRoleDataScopeRelEntity>()
                        .eq(IamRoleDataScopeRelEntity::getRoleId, roleId)
                        .eq(IamRoleDataScopeRelEntity::getDeletedFlag, 0)
                        .set(IamRoleDataScopeRelEntity::getDeletedFlag, 1)
                        .set(IamRoleDataScopeRelEntity::getUpdatedBy, actor.userId())
                        .set(IamRoleDataScopeRelEntity::getUpdatedTime, LocalDateTime.now()));

        for (Long orgId : orgIds) {
            requiredOrg(orgId);
            IamRoleDataScopeRelEntity rel = new IamRoleDataScopeRelEntity();
            applyCreateAudit(rel, actor.userId());
            rel.setRoleId(roleId);
            rel.setOrgId(orgId);
            iamRoleDataScopeRelMapper.insert(rel);
        }
        authService.evictUsersByRoleId(roleId);

        role.setDataScope(dataScope);
        return Map.of(
                "roleId", roleId,
                "dataScope", role.getDataScope(),
                "orgIds", orgIds);
    }

    @Transactional
    public void deleteRole(Long roleId, UserPrincipal actor) {
        requiredRole(roleId);
        iamRoleMapper.update(
                null,
                new LambdaUpdateWrapper<IamRoleEntity>()
                        .eq(IamRoleEntity::getId, roleId)
                        .set(IamRoleEntity::getDeletedFlag, 1)
                        .set(IamRoleEntity::getUpdatedBy, actor.userId())
                        .set(IamRoleEntity::getUpdatedTime, LocalDateTime.now()));
        authService.evictUsersByRoleId(roleId);
    }

    @Transactional
    public Map<String, Object> createOrg(Map<String, Object> request, UserPrincipal actor) {
        Long parentId = longValue(request.get("parentId"));
        if (parentId != null) {
            requiredOrg(parentId);
        }
        IamOrgEntity org = new IamOrgEntity();
        applyCreateAudit(org, actor.userId());
        org.setOrgName(requiredString(request, "orgName"));
        org.setParentId(parentId);
        iamOrgMapper.insert(org);
        return orgResponse(org);
    }

    @Transactional
    public Map<String, Object> updateOrg(Long orgId, Map<String, Object> request, UserPrincipal actor) {
        IamOrgEntity org = requiredOrg(orgId);
        Long parentId = longValue(request.get("parentId"));
        if (parentId != null) {
            requiredOrg(parentId);
        }
        iamOrgMapper.update(
                null,
                new LambdaUpdateWrapper<IamOrgEntity>()
                        .eq(IamOrgEntity::getId, orgId)
                        .set(IamOrgEntity::getOrgName, requiredString(request, "orgName"))
                        .set(IamOrgEntity::getParentId, parentId)
                        .set(IamOrgEntity::getUpdatedBy, actor.userId())
                        .set(IamOrgEntity::getUpdatedTime, LocalDateTime.now()));
        org.setOrgName(requiredString(request, "orgName"));
        org.setParentId(parentId);
        return orgResponse(org);
    }

    @Transactional
    public void deleteOrg(Long orgId, UserPrincipal actor) {
        requiredOrg(orgId);
        iamOrgMapper.update(
                null,
                new LambdaUpdateWrapper<IamOrgEntity>()
                        .eq(IamOrgEntity::getId, orgId)
                        .set(IamOrgEntity::getDeletedFlag, 1)
                        .set(IamOrgEntity::getUpdatedBy, actor.userId())
                        .set(IamOrgEntity::getUpdatedTime, LocalDateTime.now()));
    }

    @Transactional
    public Map<String, Object> createMenu(Map<String, Object> request, UserPrincipal actor) {
        String menuCode = requiredString(request, "menuCode");
        assertUniqueMenuCode(menuCode, null);
        Long parentId = longValue(request.get("parentId"));
        if (parentId != null) {
            requiredMenu(parentId);
        }
        IamMenuEntity menu = new IamMenuEntity();
        applyCreateAudit(menu, actor.userId());
        fillMenu(menu, request, true);
        iamMenuMapper.insert(menu);
        evictMenuTreeAndPermissions();
        return menuResponse(menu);
    }

    @Transactional
    public Map<String, Object> updateMenu(Long menuId, Map<String, Object> request, UserPrincipal actor) {
        IamMenuEntity menu = requiredMenu(menuId);
        Long parentId = longValue(request.get("parentId"));
        if (parentId != null) {
            requiredMenu(parentId);
        }
        fillMenu(menu, request, false);
        LocalDateTime now = LocalDateTime.now();
        iamMenuMapper.update(
                null,
                new LambdaUpdateWrapper<IamMenuEntity>()
                        .eq(IamMenuEntity::getId, menuId)
                        .set(IamMenuEntity::getMenuName, menu.getMenuName())
                        .set(IamMenuEntity::getMenuType, menu.getMenuType())
                        .set(IamMenuEntity::getPath, menu.getPath())
                        .set(IamMenuEntity::getParentId, menu.getParentId())
                        .set(IamMenuEntity::getIcon, menu.getIcon())
                        .set(IamMenuEntity::getSortNo, menu.getSortNo())
                        .set(IamMenuEntity::getVisibleFlag, menu.getVisibleFlag())
                        .set(IamMenuEntity::getPermissionCode, menu.getPermissionCode())
                        .set(IamMenuEntity::getApiPath, menu.getApiPath())
                        .set(IamMenuEntity::getComponentCode, menu.getComponentCode())
                        .set(IamMenuEntity::getUpdatedBy, actor.userId())
                        .set(IamMenuEntity::getUpdatedTime, now));
        menu.setUpdatedBy(actor.userId());
        menu.setUpdatedTime(now);
        evictMenuTreeAndPermissions();
        return menuResponse(menu);
    }

    @Transactional
    public void deleteMenu(Long menuId, UserPrincipal actor) {
        requiredMenu(menuId);
        iamMenuMapper.update(
                null,
                new LambdaUpdateWrapper<IamMenuEntity>()
                        .eq(IamMenuEntity::getId, menuId)
                        .set(IamMenuEntity::getDeletedFlag, 1)
                        .set(IamMenuEntity::getUpdatedBy, actor.userId())
                        .set(IamMenuEntity::getUpdatedTime, LocalDateTime.now()));
        evictMenuTreeAndPermissions();
    }

    private void evictMenuTreeAndPermissions() {
        redisService.delete("cp:menu:tree");
        authService.evictAllUserAuthCaches();
    }

    private void fillMenu(IamMenuEntity menu, Map<String, Object> request, boolean requireMenuCode) {
        if (requireMenuCode) {
            menu.setMenuCode(requiredString(request, "menuCode"));
        }
        menu.setMenuName(requiredString(request, "menuName"));
        menu.setMenuType(defaultString(request, "menuType", "MENU"));
        menu.setPath(defaultString(request, "path", ""));
        menu.setParentId(longValue(request.get("parentId")));
        menu.setIcon(optionalString(request, "icon"));
        menu.setSortNo(requiredInteger(request, "sortNo"));
        menu.setVisibleFlag(requiredInteger(request, "visibleFlag"));
        menu.setPermissionCode(optionalString(request, "permissionCode"));
        menu.setApiPath(optionalString(request, "apiPath"));
        menu.setComponentCode(optionalString(request, "componentCode"));
    }

    private List<String> roleCodes(Long userId) {
        return iamUserRoleRelMapper.selectList(new LambdaQueryWrapper<IamUserRoleRelEntity>()
                        .eq(IamUserRoleRelEntity::getDeletedFlag, 0)
                        .eq(IamUserRoleRelEntity::getUserId, userId))
                .stream()
                .map(IamUserRoleRelEntity::getRoleId)
                .map(this::requiredRole)
                .map(IamRoleEntity::getRoleCode)
                .toList();
    }

    private List<String> permissionCodes(Long roleId) {
        return iamRolePermissionRelMapper.selectList(new LambdaQueryWrapper<IamRolePermissionRelEntity>()
                        .eq(IamRolePermissionRelEntity::getDeletedFlag, 0)
                        .eq(IamRolePermissionRelEntity::getRoleId, roleId)
                        .orderByAsc(IamRolePermissionRelEntity::getId))
                .stream()
                .map(IamRolePermissionRelEntity::getPermissionCode)
                .toList();
    }

    private IamUserEntity requiredUser(Long userId) {
        IamUserEntity user = iamUserMapper.selectOne(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getDeletedFlag, 0)
                .eq(IamUserEntity::getId, userId));
        if (user == null) {
            throw new NotFoundException("User not found: " + userId);
        }
        return user;
    }

    private IamOrgEntity requiredOrg(Long orgId) {
        IamOrgEntity org = iamOrgMapper.selectOne(new LambdaQueryWrapper<IamOrgEntity>()
                .eq(IamOrgEntity::getDeletedFlag, 0)
                .eq(IamOrgEntity::getId, orgId));
        if (org == null) {
            throw new NotFoundException("Organization not found: " + orgId);
        }
        return org;
    }

    private IamRoleEntity requiredRole(Long roleId) {
        IamRoleEntity role = iamRoleMapper.selectOne(new LambdaQueryWrapper<IamRoleEntity>()
                .eq(IamRoleEntity::getDeletedFlag, 0)
                .eq(IamRoleEntity::getId, roleId));
        if (role == null) {
            throw new NotFoundException("Role not found: " + roleId);
        }
        return role;
    }

    private IamMenuEntity requiredMenu(Long menuId) {
        IamMenuEntity menu = iamMenuMapper.selectOne(new LambdaQueryWrapper<IamMenuEntity>()
                .eq(IamMenuEntity::getDeletedFlag, 0)
                .eq(IamMenuEntity::getId, menuId));
        if (menu == null) {
            throw new NotFoundException("Menu not found: " + menuId);
        }
        return menu;
    }

    private void assertUniqueUserLoginName(String loginName, Long excludeId) {
        IamUserEntity existing = iamUserMapper.selectOne(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getDeletedFlag, 0)
                .eq(IamUserEntity::getLoginName, loginName));
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new BadRequestException("Login name already exists: " + loginName);
        }
    }

    private void assertUniqueRoleCode(String roleCode, Long excludeId) {
        IamRoleEntity existing = iamRoleMapper.selectOne(new LambdaQueryWrapper<IamRoleEntity>()
                .eq(IamRoleEntity::getDeletedFlag, 0)
                .eq(IamRoleEntity::getRoleCode, roleCode));
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new BadRequestException("Role code already exists: " + roleCode);
        }
    }

    private void assertUniqueMenuCode(String menuCode, Long excludeId) {
        IamMenuEntity existing = iamMenuMapper.selectOne(new LambdaQueryWrapper<IamMenuEntity>()
                .eq(IamMenuEntity::getDeletedFlag, 0)
                .eq(IamMenuEntity::getMenuCode, menuCode));
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new BadRequestException("Menu code already exists: " + menuCode);
        }
    }

    private void applyCreateAudit(io.cloudpivot.common.persistence.BaseEntity entity, long actorUserId) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedBy(actorUserId);
        entity.setCreatedTime(now);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        entity.setDeletedFlag(0);
        entity.setVersionNo(0L);
    }

    private long authVersion(IamUserEntity user) {
        return user.getAuthVersion() == null ? 0L : user.getAuthVersion();
    }

    private String resourceType(String permissionCode) {
        if (permissionCode.startsWith("menu:")) {
            return "MENU";
        }
        if (permissionCode.startsWith("btn:")) {
            return "BUTTON";
        }
        return "API";
    }

    private Map<String, Object> userResponse(IamUserEntity user, IamOrgEntity org, List<String> roles) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("userName", user.getUserName());
        response.put("loginName", user.getLoginName());
        response.put("orgId", user.getOrgId());
        response.put("orgName", org == null ? "" : org.getOrgName());
        response.put("phone", user.getPhone());
        response.put("email", user.getEmail());
        response.put("userStatus", user.getUserStatus());
        response.put("roles", roles);
        return response;
    }

    private Map<String, Object> roleResponse(IamRoleEntity role, List<String> permissions) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", role.getId());
        response.put("roleCode", role.getRoleCode());
        response.put("roleName", role.getRoleName());
        response.put("dataScope", role.getDataScope());
        response.put("permissionCodes", permissions);
        return response;
    }

    private Map<String, Object> orgResponse(IamOrgEntity org) {
        return Map.of(
                "id", org.getId(),
                "orgName", org.getOrgName(),
                "parentId", org.getParentId());
    }

    private Map<String, Object> menuResponse(IamMenuEntity menu) {
        return Map.of(
                "id", menu.getId(),
                "menuCode", menu.getMenuCode(),
                "menuName", menu.getMenuName(),
                "menuType", menu.getMenuType(),
                "path", menu.getPath(),
                "parentId", menu.getParentId(),
                "icon", menu.getIcon(),
                "sortNo", menu.getSortNo(),
                "visibleFlag", menu.getVisibleFlag(),
                "permissionCode", menu.getPermissionCode());
    }

    private String requiredString(Map<String, Object> request, String key) {
        String value = optionalString(request, key);
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Field is required: " + key);
        }
        return value;
    }

    private String defaultString(Map<String, Object> request, String key, String defaultValue) {
        String value = optionalString(request, key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String optionalString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long requiredLong(Map<String, Object> request, String key) {
        Long value = longValue(request.get(key));
        if (value == null) {
            throw new BadRequestException("Field is required: " + key);
        }
        return value;
    }

    private Integer requiredInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            throw new BadRequestException("Field is required: " + key);
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longValue(Object value) {
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }
}
