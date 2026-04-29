package io.cloudpivot.auth.service.store;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudpivot.auth.service.RedisService;

@Component
public class UserAuthCache {

    private static final String DETAIL_KEY_PREFIX = "cp:user:detail:";
    private static final String ROLES_KEY_PREFIX = "cp:user:roles:";
    private static final String PERMS_KEY_PREFIX = "cp:user:perms:";
    private static final String DATA_SCOPE_KEY_PREFIX = "cp:user:data-scope:";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public UserAuthCache(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public Optional<UserAuthSnapshot> get(long userId) {
        UserDetailSnapshot detail = read(detailKey(userId), UserDetailSnapshot.class);
        List<String> roles = readList(rolesKey(userId));
        List<String> permissions = readList(permsKey(userId));
        if (detail == null || roles == null || permissions == null) {
            return Optional.empty();
        }
        return Optional.of(new UserAuthSnapshot(
                detail.userId(),
                detail.userName(),
                detail.orgId(),
                detail.orgName(),
                roles,
                permissions,
                detail.superAdmin(),
                detail.authVersion()));
    }

    public void put(UserAuthSnapshot snapshot, Duration ttl) {
        write(detailKey(snapshot.userId()), new UserDetailSnapshot(
                snapshot.userId(),
                snapshot.userName(),
                snapshot.orgId(),
                snapshot.orgName(),
                snapshot.superAdmin(),
                snapshot.authVersion()), ttl);
        write(rolesKey(snapshot.userId()), snapshot.roles(), ttl);
        write(permsKey(snapshot.userId()), snapshot.permissions(), ttl);
    }

    public Optional<String> getDataScope(long userId) {
        return Optional.ofNullable(redisService.get(dataScopeKey(userId)));
    }

    public void putDataScope(long userId, String dataScope, Duration ttl) {
        redisService.set(dataScopeKey(userId), dataScope, ttl);
    }

    public void evictUser(long userId) {
        redisService.delete(detailKey(userId));
        redisService.delete(rolesKey(userId));
        redisService.delete(permsKey(userId));
        redisService.delete(dataScopeKey(userId));
    }

    private String detailKey(long userId) {
        return DETAIL_KEY_PREFIX + userId;
    }

    private String rolesKey(long userId) {
        return ROLES_KEY_PREFIX + userId;
    }

    private String permsKey(long userId) {
        return PERMS_KEY_PREFIX + userId;
    }

    private String dataScopeKey(long userId) {
        return DATA_SCOPE_KEY_PREFIX + userId;
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redisService.set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to cache user auth snapshot.", exception);
        }
    }

    private <T> T read(String key, Class<T> valueType) {
        String json = redisService.get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read user auth snapshot.", exception);
        }
    }

    private List<String> readList(String key) {
        String json = redisService.get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read cached permissions.", exception);
        }
    }

    public record UserAuthSnapshot(
            long userId,
            String userName,
            Long orgId,
            String orgName,
            List<String> roles,
            List<String> permissions,
            boolean superAdmin,
            long authVersion) {
    }

    private record UserDetailSnapshot(
            long userId,
            String userName,
            Long orgId,
            String orgName,
            boolean superAdmin,
            long authVersion) {
    }
}
