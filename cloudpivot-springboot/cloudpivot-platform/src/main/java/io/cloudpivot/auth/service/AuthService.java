package io.cloudpivot.auth.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import io.cloudpivot.auth.api.dto.ChangePasswordRequest;
import io.cloudpivot.auth.api.dto.CurrentUserResponse;
import io.cloudpivot.auth.api.dto.LoginRequest;
import io.cloudpivot.auth.api.dto.LoginResponse;
import io.cloudpivot.auth.api.dto.RefreshTokenRequest;
import io.cloudpivot.auth.api.dto.TokenRefreshResponse;
import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.auth.service.JwtService.AccessClaims;
import io.cloudpivot.auth.service.JwtService.RefreshClaims;
import io.cloudpivot.auth.service.JwtService.TokenPair;
import io.cloudpivot.auth.service.store.LoginFailCounter;
import io.cloudpivot.auth.service.store.RefreshSessionStore;
import io.cloudpivot.auth.service.store.RefreshSessionStore.RefreshSession;
import io.cloudpivot.auth.service.store.TokenBlacklistStore;
import io.cloudpivot.auth.service.store.UserAuthCache;
import io.cloudpivot.auth.service.store.UserAuthCache.UserAuthSnapshot;
import io.cloudpivot.iam.persistence.entity.IamOrgEntity;
import io.cloudpivot.iam.persistence.entity.IamRoleEntity;
import io.cloudpivot.iam.persistence.entity.IamRolePermissionRelEntity;
import io.cloudpivot.iam.persistence.entity.IamUserEntity;
import io.cloudpivot.iam.persistence.entity.IamUserRoleRelEntity;
import io.cloudpivot.iam.persistence.mapper.IamOrgMapper;
import io.cloudpivot.iam.persistence.mapper.IamRoleMapper;
import io.cloudpivot.iam.persistence.mapper.IamRolePermissionRelMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserMapper;
import io.cloudpivot.iam.persistence.mapper.IamUserRoleRelMapper;
import io.cloudpivot.system.persistence.entity.SysLoginLogEntity;
import io.cloudpivot.system.persistence.mapper.SysLoginLogMapper;

@Service
public class AuthService {

    private final IamUserMapper iamUserMapper;
    private final IamOrgMapper iamOrgMapper;
    private final IamRoleMapper iamRoleMapper;
    private final IamUserRoleRelMapper iamUserRoleRelMapper;
    private final IamRolePermissionRelMapper iamRolePermissionRelMapper;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final RefreshSessionStore refreshSessionStore;
    private final TokenBlacklistStore tokenBlacklistStore;
    private final LoginFailCounter loginFailCounter;
    private final UserAuthCache userAuthCache;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final int loginFailThreshold;
    private final long loginLockDurationSeconds;
    private final Duration authCacheTtl;

    public AuthService(
            IamUserMapper iamUserMapper,
            IamOrgMapper iamOrgMapper,
            IamRoleMapper iamRoleMapper,
            IamUserRoleRelMapper iamUserRoleRelMapper,
            IamRolePermissionRelMapper iamRolePermissionRelMapper,
            PasswordService passwordService,
            JwtService jwtService,
            RefreshSessionStore refreshSessionStore,
            TokenBlacklistStore tokenBlacklistStore,
            LoginFailCounter loginFailCounter,
            UserAuthCache userAuthCache,
            SysLoginLogMapper sysLoginLogMapper,
            @Value("${cloudpivot.auth.login-fail-threshold:5}") int loginFailThreshold,
            @Value("${cloudpivot.auth.login-lock-duration-seconds:1800}") long loginLockDurationSeconds,
            @Value("${cloudpivot.auth.cache-ttl-seconds:1800}") long authCacheTtlSeconds) {
        this.iamUserMapper = iamUserMapper;
        this.iamOrgMapper = iamOrgMapper;
        this.iamRoleMapper = iamRoleMapper;
        this.iamUserRoleRelMapper = iamUserRoleRelMapper;
        this.iamRolePermissionRelMapper = iamRolePermissionRelMapper;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.refreshSessionStore = refreshSessionStore;
        this.tokenBlacklistStore = tokenBlacklistStore;
        this.loginFailCounter = loginFailCounter;
        this.userAuthCache = userAuthCache;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.loginFailThreshold = loginFailThreshold;
        this.loginLockDurationSeconds = loginLockDurationSeconds;
        this.authCacheTtl = Duration.ofSeconds(authCacheTtlSeconds);
    }

    public LoginResponse login(LoginRequest request) {
        String loginName = request.resolvedLoginName();
        if (loginName == null || loginName.isBlank() || request.password() == null || request.password().isBlank()) {
            throw new UnauthorizedException("Login credentials are invalid.");
        }

        IamUserEntity user = iamUserMapper.selectOne(new LambdaQueryWrapper<IamUserEntity>()
                .eq(IamUserEntity::getDeletedFlag, 0)
                .eq(IamUserEntity::getLoginName, loginName));

        if (user == null) {
            registerLoginFailure(loginName, null, "FAIL_PASSWORD");
            throw new UnauthorizedException("Login credentials are invalid.");
        }

        try {
            ensureUserCanAuthenticate(user);
        } catch (UnauthorizedException exception) {
            registerLoginResult(user, "LOGIN", "ENABLED".equalsIgnoreCase(user.getUserStatus()) ? "FAIL_LOCKED" : "FAIL_DISABLED", null, null);
            throw exception;
        }
        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            registerLoginFailure(loginName, user, "FAIL_PASSWORD");
            throw new UnauthorizedException("Login credentials are invalid.");
        }

        loginFailCounter.clear(loginName);
        LocalDateTime now = LocalDateTime.now();
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, user.getId())
                        .set(IamUserEntity::getLockExpireTime, null)
                        .set(IamUserEntity::getLastLoginTime, now)
                        .set(IamUserEntity::getUpdatedBy, user.getId())
                        .set(IamUserEntity::getUpdatedTime, now));

        UserAuthSnapshot snapshot = snapshotByUser(user);
        TokenPair tokenPair = jwtService.issueTokens(
                user.getId(),
                user.getUserName(),
                snapshot.roles(),
                authVersionOf(user));

        refreshSessionStore.save(new RefreshSession(
                tokenPair.sessionId(),
                tokenPair.refreshTokenId(),
                user.getId(),
                authVersionOf(user),
                tokenPair.refreshExpiresAt()));
        userAuthCache.put(snapshot, authCacheTtl);
        registerLoginResult(user, "LOGIN", "SUCCESS", tokenPair.accessTokenId(), tokenPair.sessionId());

        return new LoginResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn(),
                user.getId(),
                user.getUserName(),
                snapshot.roles());
    }

    public TokenRefreshResponse refresh(RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is invalid.");
        }

        RefreshClaims claims = jwtService.parseRefreshToken(request.refreshToken());
        if (!refreshSessionStore.matches(
                claims.sessionId(),
                claims.tokenId(),
                claims.userId(),
                claims.authVersion())) {
            throw new UnauthorizedException("Refresh token is invalid.");
        }

        IamUserEntity user = iamUserMapper.selectById(claims.userId());
        if (user == null || authVersionOf(user) != claims.authVersion()) {
            refreshSessionStore.delete(claims.sessionId());
            throw new UnauthorizedException("Refresh token is invalid.");
        }

        ensureUserCanAuthenticate(user);
        UserAuthSnapshot snapshot = snapshotByUser(user);
        refreshSessionStore.delete(claims.sessionId());
        TokenPair tokenPair = jwtService.issueTokens(
                user.getId(),
                user.getUserName(),
                snapshot.roles(),
                authVersionOf(user));

        refreshSessionStore.save(new RefreshSession(
                tokenPair.sessionId(),
                tokenPair.refreshTokenId(),
                user.getId(),
                authVersionOf(user),
                tokenPair.refreshExpiresAt()));
        userAuthCache.put(snapshot, authCacheTtl);
        registerLoginResult(user, "REFRESH", "SUCCESS", tokenPair.accessTokenId(), tokenPair.sessionId());

        return new TokenRefreshResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn());
    }

    public void logout(String accessToken) {
        AccessClaims claims = jwtService.parseAccessToken(accessToken);
        tokenBlacklistStore.block(claims.tokenId(), claims.expiresAt());
        refreshSessionStore.delete(claims.sessionId());
        userAuthCache.evictUser(claims.userId());
        IamUserEntity user = iamUserMapper.selectById(claims.userId());
        if (user != null) {
            registerLoginResult(user, "LOGOUT", "SUCCESS", claims.tokenId(), claims.sessionId());
        }
    }

    public void changePassword(Long userId, ChangePasswordRequest request, String accessToken) {
        IamUserEntity user = iamUserMapper.selectById(userId);
        if (user == null) {
            throw new UnauthorizedException("User is unavailable.");
        }
        if (!passwordService.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is invalid.");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new UnauthorizedException("New password is invalid.");
        }

        long nextAuthVersion = authVersionOf(user) + 1;
        iamUserMapper.update(
                null,
                new LambdaUpdateWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getId, userId)
                        .eq(IamUserEntity::getDeletedFlag, 0)
                        .set(IamUserEntity::getPasswordHash, passwordService.encode(request.newPassword()))
                        .set(IamUserEntity::getAuthVersion, nextAuthVersion)
                        .set(IamUserEntity::getUpdatedBy, userId)
                        .set(IamUserEntity::getUpdatedTime, LocalDateTime.now()));

        refreshSessionStore.deleteByUserId(userId);
        userAuthCache.evictUser(userId);

        AccessClaims claims = jwtService.parseAccessToken(accessToken);
        tokenBlacklistStore.block(claims.tokenId(), claims.expiresAt());
        registerLoginResult(user, "CHANGE_PASSWORD", "SUCCESS", claims.tokenId(), claims.sessionId());
    }

    public CurrentUserResponse currentUser(UserPrincipal principal) {
        return new CurrentUserResponse(
                principal.userId(),
                principal.userName(),
                principal.orgName(),
                principal.roles(),
                principal.permissions());
    }

    public Optional<UserPrincipal> findByAccessToken(String accessToken) {
        try {
            AccessClaims claims = jwtService.parseAccessToken(accessToken);
            if (tokenBlacklistStore.isBlocked(claims.tokenId())) {
                return Optional.empty();
            }

            Optional<UserAuthSnapshot> cachedSnapshot = userAuthCache.get(claims.userId());
            if (cachedSnapshot.isPresent() && cachedSnapshot.get().authVersion() == claims.authVersion()) {
                return Optional.of(toPrincipal(cachedSnapshot.get()));
            }

            IamUserEntity user = iamUserMapper.selectById(claims.userId());
            if (user == null || authVersionOf(user) != claims.authVersion()) {
                userAuthCache.evictUser(claims.userId());
                return Optional.empty();
            }

            ensureUserCanAuthenticate(user);
            UserAuthSnapshot snapshot = snapshotByUser(user);
            userAuthCache.put(snapshot, authCacheTtl);
            return Optional.of(toPrincipal(snapshot));
        } catch (UnauthorizedException exception) {
            return Optional.empty();
        }
    }

    public void revokeUserSessions(long userId) {
        refreshSessionStore.deleteByUserId(userId);
        userAuthCache.evictUser(userId);
    }

    public void evictUserAuthCache(long userId) {
        userAuthCache.evictUser(userId);
    }

    public void evictUsersByRoleId(long roleId) {
        Set<Long> userIds = iamUserRoleRelMapper.selectList(new LambdaQueryWrapper<IamUserRoleRelEntity>()
                        .eq(IamUserRoleRelEntity::getDeletedFlag, 0)
                        .eq(IamUserRoleRelEntity::getRoleId, roleId))
                .stream()
                .map(IamUserRoleRelEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        userIds.forEach(userAuthCache::evictUser);
    }

    public void evictAllUserAuthCaches() {
        iamUserMapper.selectList(new LambdaQueryWrapper<IamUserEntity>()
                        .eq(IamUserEntity::getDeletedFlag, 0))
                .stream()
                .map(IamUserEntity::getId)
                .forEach(userAuthCache::evictUser);
    }

    private void ensureUserCanAuthenticate(IamUserEntity user) {
        if (!"ENABLED".equalsIgnoreCase(user.getUserStatus())) {
            throw new UnauthorizedException("User account is disabled.");
        }
        if (user.getLockExpireTime() != null && user.getLockExpireTime().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("User account is locked.");
        }
    }

    private long authVersionOf(IamUserEntity user) {
        return user.getAuthVersion() == null ? 0L : user.getAuthVersion();
    }

    private UserAuthSnapshot snapshotByUser(IamUserEntity user) {
        IamOrgEntity org = iamOrgMapper.selectById(user.getOrgId());
        List<IamRoleEntity> roleEntities = rolesByUserId(user.getId());
        List<String> roleCodes = roleEntities.stream()
                .map(IamRoleEntity::getRoleCode)
                .toList();
        List<String> permissions = permissionsByRoleIds(roleEntities.stream().map(IamRoleEntity::getId).toList());
        return new UserAuthSnapshot(
                user.getId(),
                user.getUserName(),
                user.getOrgId(),
                org == null ? "" : org.getOrgName(),
                roleCodes,
                permissions,
                user.getSuperAdminFlag() != null && user.getSuperAdminFlag() == 1,
                authVersionOf(user));
    }

    private List<IamRoleEntity> rolesByUserId(Long userId) {
        List<IamUserRoleRelEntity> relations = iamUserRoleRelMapper.selectList(new LambdaQueryWrapper<IamUserRoleRelEntity>()
                .eq(IamUserRoleRelEntity::getDeletedFlag, 0)
                .eq(IamUserRoleRelEntity::getUserId, userId)
                .orderByAsc(IamUserRoleRelEntity::getId));

        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = relations.stream()
                .map(IamUserRoleRelEntity::getRoleId)
                .toList();

        Map<Long, IamRoleEntity> roleById = iamRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(IamRoleEntity::getId, Function.identity()));

        return relations.stream()
                .map(relation -> roleById.get(relation.getRoleId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> permissionsByRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }

        return iamRolePermissionRelMapper.selectList(new LambdaQueryWrapper<IamRolePermissionRelEntity>()
                        .eq(IamRolePermissionRelEntity::getDeletedFlag, 0)
                        .in(IamRolePermissionRelEntity::getRoleId, roleIds)
                        .orderByAsc(IamRolePermissionRelEntity::getId))
                .stream()
                .map(IamRolePermissionRelEntity::getPermissionCode)
                .filter(permission -> permission != null && !permission.isBlank())
                .distinct()
                .toList();
    }

    private UserPrincipal toPrincipal(UserAuthSnapshot snapshot) {
        return new UserPrincipal(
                snapshot.userId(),
                snapshot.userName(),
                snapshot.orgId(),
                snapshot.orgName(),
                snapshot.roles(),
                snapshot.permissions(),
                snapshot.superAdmin());
    }

    private void registerLoginFailure(String loginName, IamUserEntity user, String resultCode) {
        long count = loginFailCounter.increment(loginName, Duration.ofSeconds(loginLockDurationSeconds));
        if (user != null && count >= loginFailThreshold) {
            LocalDateTime lockExpireTime = LocalDateTime.now().plusSeconds(loginLockDurationSeconds);
            iamUserMapper.update(
                    null,
                    new LambdaUpdateWrapper<IamUserEntity>()
                            .eq(IamUserEntity::getId, user.getId())
                            .set(IamUserEntity::getLockExpireTime, lockExpireTime)
                            .set(IamUserEntity::getUpdatedBy, user.getId())
                            .set(IamUserEntity::getUpdatedTime, LocalDateTime.now()));
        }
        registerLoginResult(user, "LOGIN", resultCode, null, null, loginName);
    }

    private void registerLoginResult(IamUserEntity user, String actionCode, String resultCode, String tokenJti, String sessionId) {
        registerLoginResult(user, actionCode, resultCode, tokenJti, sessionId, user == null ? null : user.getLoginName());
    }

    private void registerLoginResult(
            IamUserEntity user,
            String actionCode,
            String resultCode,
            String tokenJti,
            String sessionId,
            String loginName) {
        SysLoginLogEntity log = new SysLoginLogEntity();
        log.setUserId(user == null ? null : user.getId());
        log.setLoginName(loginName == null ? "" : loginName);
        log.setActionCode(actionCode);
        log.setResultCode(resultCode);
        log.setTokenJti(tokenJti);
        log.setSessionId(sessionId);
        log.setEventTime(LocalDateTime.now());
        sysLoginLogMapper.insert(log);
    }

}
