package io.cloudpivot.iam.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudpivot.auth.service.RedisService;
import io.cloudpivot.common.api.PageResponse;
import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.iam.api.dto.MenuNode;
import io.cloudpivot.iam.api.dto.RoleSummary;
import io.cloudpivot.iam.api.dto.UserSummary;
import io.cloudpivot.iam.security.DataScopeService;
import io.cloudpivot.iam.security.DataScopeService.DataScope;
import io.cloudpivot.iam.persistence.entity.IamMenuEntity;
import io.cloudpivot.iam.persistence.entity.IamOrgEntity;
import io.cloudpivot.iam.persistence.entity.IamRoleEntity;
import io.cloudpivot.iam.persistence.entity.IamUserEntity;
import io.cloudpivot.iam.persistence.entity.IamUserRoleRelEntity;
import io.cloudpivot.iam.persistence.mapper.IamMenuMapper;
import io.cloudpivot.iam.persistence.mapper.IamOrgMapper;
import io.cloudpivot.iam.persistence.mapper.IamRoleMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserRoleRelMapper;

@Service
public class IamQueryService {

    private static final String MENU_TREE_CACHE_KEY = "cp:menu:tree";

    private final IamMenuMapper iamMenuMapper;
    private final IamUserMapper iamUserMapper;
    private final IamRoleMapper iamRoleMapper;
    private final IamOrgMapper iamOrgMapper;
    private final IamUserRoleRelMapper iamUserRoleRelMapper;
    private final DataScopeService dataScopeService;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public IamQueryService(
            IamMenuMapper iamMenuMapper,
            IamUserMapper iamUserMapper,
            IamRoleMapper iamRoleMapper,
            IamOrgMapper iamOrgMapper,
            IamUserRoleRelMapper iamUserRoleRelMapper,
            DataScopeService dataScopeService,
            RedisService redisService,
            ObjectMapper objectMapper) {
        this.iamMenuMapper = iamMenuMapper;
        this.iamUserMapper = iamUserMapper;
        this.iamRoleMapper = iamRoleMapper;
        this.iamOrgMapper = iamOrgMapper;
        this.iamUserRoleRelMapper = iamUserRoleRelMapper;
        this.dataScopeService = dataScopeService;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public List<MenuNode> menuTree() {
        String cachedMenuTree = redisService.get(MENU_TREE_CACHE_KEY);
        if (cachedMenuTree != null && !cachedMenuTree.isBlank()) {
            try {
                return objectMapper.readValue(cachedMenuTree, new TypeReference<List<MenuNode>>() {
                });
            } catch (JsonProcessingException exception) {
                redisService.delete(MENU_TREE_CACHE_KEY);
            }
        }

        List<IamMenuEntity> menus = iamMenuMapper.selectList(new LambdaQueryWrapper<IamMenuEntity>()
                .eq(IamMenuEntity::getDeletedFlag, 0)
                .orderByAsc(IamMenuEntity::getSortNo, IamMenuEntity::getId));

        Map<Long, List<IamMenuEntity>> childrenByParentId = menus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(IamMenuEntity::getParentId));

        List<MenuNode> menuTree = menus.stream()
                .filter(menu -> menu.getParentId() == null)
                .map(menu -> toMenuNode(menu, childrenByParentId))
                .toList();
        try {
            redisService.set(MENU_TREE_CACHE_KEY, objectMapper.writeValueAsString(menuTree), java.time.Duration.ofHours(1));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to cache menu tree.", exception);
        }
        return menuTree;
    }

    public PageResponse<UserSummary> users(UserPrincipal principal) {
        List<IamUserEntity> users = iamUserMapper.selectList(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getDeletedFlag, 0)
                .orderByAsc(IamUserEntity::getId));
        DataScope dataScope = dataScopeService.accessibleScope(principal);
        users = users.stream()
                .filter(user -> matchesScope(user, dataScope))
                .toList();

        Map<Long, IamOrgEntity> orgById = iamOrgMapper.selectBatchIds(users.stream()
                        .map(IamUserEntity::getOrgId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(IamOrgEntity::getId, Function.identity()));

        Map<Long, List<String>> roleCodesByUserId = roleCodesByUserId(users.stream()
                .map(IamUserEntity::getId)
                .toList());

        List<UserSummary> records = users.stream()
                .map(user -> new UserSummary(
                        user.getId(),
                        user.getUserName(),
                        user.getLoginName(),
                        orgById.containsKey(user.getOrgId()) ? orgById.get(user.getOrgId()).getOrgName() : "",
                        user.getUserStatus(),
                        roleCodesByUserId.getOrDefault(user.getId(), List.of())))
                .toList();

        return new PageResponse<>(records, records.size());
    }

    private boolean matchesScope(IamUserEntity user, DataScope scope) {
        if (scope.all()) {
            return true;
        }
        if (!scope.orgIds().isEmpty()) {
            return scope.orgIds().contains(user.getOrgId());
        }
        if (scope.selfUserId() != null) {
            return Objects.equals(scope.selfUserId(), user.getId());
        }
        return false;
    }

    public List<RoleSummary> roles() {
        return iamRoleMapper.selectList(new LambdaQueryWrapper<IamRoleEntity>()
                        .eq(IamRoleEntity::getDeletedFlag, 0)
                        .orderByAsc(IamRoleEntity::getId))
                .stream()
                .map(role -> new RoleSummary(
                        role.getId(),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getDataScope()))
                .toList();
    }

    private MenuNode toMenuNode(IamMenuEntity menu, Map<Long, List<IamMenuEntity>> childrenByParentId) {
        List<MenuNode> children = childrenByParentId.getOrDefault(menu.getId(), List.of())
                .stream()
                .map(child -> toMenuNode(child, childrenByParentId))
                .toList();

        return new MenuNode(menu.getMenuCode(), menu.getMenuName(), menu.getPath(), children);
    }

    private Map<Long, List<String>> roleCodesByUserId(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<IamUserRoleRelEntity> relations = iamUserRoleRelMapper.selectList(new LambdaQueryWrapper<IamUserRoleRelEntity>()
                .eq(IamUserRoleRelEntity::getDeletedFlag, 0)
                .in(IamUserRoleRelEntity::getUserId, userIds)
                .orderByAsc(IamUserRoleRelEntity::getId));

        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, IamRoleEntity> roleById = iamRoleMapper.selectBatchIds(relations.stream()
                        .map(IamUserRoleRelEntity::getRoleId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(IamRoleEntity::getId, Function.identity()));

        return relations.stream()
                .filter(relation -> roleById.containsKey(relation.getRoleId()))
                .collect(Collectors.groupingBy(
                        IamUserRoleRelEntity::getUserId,
                        Collectors.mapping(relation -> roleById.get(relation.getRoleId()).getRoleCode(), Collectors.toList())));
    }
}
