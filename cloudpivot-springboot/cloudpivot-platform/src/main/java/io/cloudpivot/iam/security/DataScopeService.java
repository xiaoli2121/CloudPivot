package io.cloudpivot.iam.security;

import java.util.ArrayDeque;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.auth.service.store.UserAuthCache;
import io.cloudpivot.iam.persistence.entity.IamOrgEntity;
import io.cloudpivot.iam.persistence.entity.IamRoleDataScopeRelEntity;
import io.cloudpivot.iam.persistence.entity.IamRoleEntity;
import io.cloudpivot.iam.persistence.entity.IamUserRoleRelEntity;
import io.cloudpivot.iam.persistence.mapper.IamOrgMapper;
import io.cloudpivot.iam.persistence.mapper.IamRoleDataScopeRelMapper;
import io.cloudpivot.iam.persistence.mapper.IamRoleMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserRoleRelMapper;

@Service
public class DataScopeService {

    private final IamUserRoleRelMapper iamUserRoleRelMapper;
    private final IamRoleMapper iamRoleMapper;
    private final IamRoleDataScopeRelMapper iamRoleDataScopeRelMapper;
    private final IamOrgMapper iamOrgMapper;
    private final UserAuthCache userAuthCache;
    private final Duration cacheTtl;

    public DataScopeService(
            IamUserRoleRelMapper iamUserRoleRelMapper,
            IamRoleMapper iamRoleMapper,
            IamRoleDataScopeRelMapper iamRoleDataScopeRelMapper,
            IamOrgMapper iamOrgMapper,
            UserAuthCache userAuthCache,
            @Value("${cloudpivot.auth.cache-ttl-seconds:1800}") long cacheTtlSeconds) {
        this.iamUserRoleRelMapper = iamUserRoleRelMapper;
        this.iamRoleMapper = iamRoleMapper;
        this.iamRoleDataScopeRelMapper = iamRoleDataScopeRelMapper;
        this.iamOrgMapper = iamOrgMapper;
        this.userAuthCache = userAuthCache;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public DataScope accessibleScope(UserPrincipal principal) {
        if (principal.superAdmin()) {
            return DataScope.allScope();
        }
        return userAuthCache.getDataScope(principal.userId())
                .map(this::parseScope)
                .orElseGet(() -> {
                    DataScope computedScope = computeAccessibleScope(principal);
                    userAuthCache.putDataScope(principal.userId(), computedScope.describe(), cacheTtl);
                    return computedScope;
                });
    }

    private DataScope computeAccessibleScope(UserPrincipal principal) {
        List<Long> roleIds = iamUserRoleRelMapper.selectList(new LambdaQueryWrapper<IamUserRoleRelEntity>()
                        .eq(IamUserRoleRelEntity::getDeletedFlag, 0)
                        .eq(IamUserRoleRelEntity::getUserId, principal.userId()))
                .stream()
                .map(IamUserRoleRelEntity::getRoleId)
                .toList();

        if (roleIds.isEmpty()) {
            return DataScope.self(principal.userId());
        }

        List<IamRoleEntity> roles = iamRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getDeletedFlag() != null && role.getDeletedFlag() == 0)
                .toList();

        boolean all = roles.stream().anyMatch(role -> "ALL".equalsIgnoreCase(role.getDataScope()));
        if (all) {
            return DataScope.allScope();
        }

        Set<Long> orgIds = new HashSet<>();
        boolean selfOnly = false;

        for (IamRoleEntity role : roles) {
            String scope = role.getDataScope();
            if ("ORG_AND_CHILDREN".equalsIgnoreCase(scope)) {
                addOrgAndChildren(orgIds, principal.orgId());
            } else if ("ORG".equalsIgnoreCase(scope)) {
                if (principal.orgId() != null) {
                    orgIds.add(principal.orgId());
                }
            } else if ("CUSTOM".equalsIgnoreCase(scope)) {
                iamRoleDataScopeRelMapper.selectList(new LambdaQueryWrapper<IamRoleDataScopeRelEntity>()
                                .eq(IamRoleDataScopeRelEntity::getDeletedFlag, 0)
                                .eq(IamRoleDataScopeRelEntity::getRoleId, role.getId()))
                        .forEach(rel -> orgIds.add(rel.getOrgId()));
            } else if ("SELF".equalsIgnoreCase(scope)) {
                selfOnly = true;
            }
        }

        if (!orgIds.isEmpty()) {
            return DataScope.orgs(orgIds);
        }
        if (selfOnly) {
            return DataScope.self(principal.userId());
        }
        return DataScope.self(principal.userId());
    }

    private DataScope parseScope(String description) {
        if (description == null || description.isBlank()) {
            return DataScope.self(null);
        }
        if ("ALL".equalsIgnoreCase(description)) {
            return DataScope.allScope();
        }
        if (description.startsWith("SELF:")) {
            return DataScope.self(Long.parseLong(description.substring("SELF:".length())));
        }
        Set<Long> orgIds = java.util.Arrays.stream(description.split(","))
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        return DataScope.orgs(orgIds);
    }

    private void addOrgAndChildren(Set<Long> collector, Long rootOrgId) {
        if (rootOrgId == null) {
            return;
        }
        List<IamOrgEntity> orgs = iamOrgMapper.selectList(new LambdaQueryWrapper<IamOrgEntity>()
                .eq(IamOrgEntity::getDeletedFlag, 0));
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootOrgId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            if (!collector.add(current)) {
                continue;
            }
            orgs.stream()
                    .filter(org -> current.equals(org.getParentId()))
                    .map(IamOrgEntity::getId)
                    .forEach(queue::addLast);
        }
    }

    public record DataScope(boolean all, Set<Long> orgIds, Long selfUserId) {

        public static DataScope allScope() {
            return new DataScope(true, Set.of(), null);
        }

        public static DataScope orgs(Set<Long> orgIds) {
            return new DataScope(false, Set.copyOf(orgIds), null);
        }

        public static DataScope self(Long userId) {
            return new DataScope(false, Set.of(), userId);
        }

        public String describe() {
            if (all) {
                return "ALL";
            }
            if (!orgIds.isEmpty()) {
                return orgIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            }
            return selfUserId == null ? "" : "SELF:" + selfUserId;
        }
    }
}
