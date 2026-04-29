package io.cloudpivot.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final boolean redisEnabled;
    private final Map<String, String> valueStore = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> setStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> expiresAtByKey = new ConcurrentHashMap<>();

    public RedisService(
            @Qualifier("cloudPivotRedisTemplate") ObjectProvider<RedisTemplate<String, String>> redisTemplateProvider,
            @Value("${cloudpivot.auth.state-store:memory}") String stateStore) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.redisEnabled = "redis".equalsIgnoreCase(stateStore) && this.redisTemplate != null;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void set(String key, String value, Duration ttl) {
        if (redisEnabled) {
            redisTemplate.opsForValue().set(key, value, ttl);
            return;
        }
        valueStore.put(key, value);
        expiresAtByKey.put(key, Instant.now().plus(ttl));
        setStore.remove(key);
    }

    public String get(String key) {
        if (redisEnabled) {
            return redisTemplate.opsForValue().get(key);
        }
        evictIfExpired(key);
        return valueStore.get(key);
    }

    public void delete(String key) {
        if (redisEnabled) {
            redisTemplate.delete(key);
            return;
        }
        valueStore.remove(key);
        setStore.remove(key);
        expiresAtByKey.remove(key);
    }

    public boolean exists(String key) {
        if (redisEnabled) {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        }
        evictIfExpired(key);
        return valueStore.containsKey(key) || setStore.containsKey(key);
    }

    public long increment(String key, Duration ttl) {
        if (redisEnabled) {
            Long nextValue = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, ttl);
            return nextValue == null ? 0L : nextValue;
        }
        evictIfExpired(key);
        long nextValue = Long.parseLong(valueStore.getOrDefault(key, "0")) + 1L;
        valueStore.put(key, String.valueOf(nextValue));
        expiresAtByKey.put(key, Instant.now().plus(ttl));
        return nextValue;
    }

    public Set<String> members(String key) {
        if (redisEnabled) {
            Set<String> members = redisTemplate.opsForSet().members(key);
            return members == null ? Set.of() : Set.copyOf(members);
        }
        evictIfExpired(key);
        Set<String> members = setStore.get(key);
        return members == null ? Set.of() : Set.copyOf(members);
    }

    public void addMember(String key, String value, Duration ttl) {
        if (redisEnabled) {
            redisTemplate.opsForSet().add(key, value);
            redisTemplate.expire(key, ttl);
            return;
        }
        evictIfExpired(key);
        setStore.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(value);
        expiresAtByKey.put(key, Instant.now().plus(ttl));
        valueStore.remove(key);
    }

    public void removeMember(String key, String value) {
        if (redisEnabled) {
            redisTemplate.opsForSet().remove(key, value);
            return;
        }
        evictIfExpired(key);
        Set<String> members = setStore.get(key);
        if (members == null) {
            return;
        }
        members.remove(value);
        if (members.isEmpty()) {
            setStore.remove(key);
            expiresAtByKey.remove(key);
        }
    }

    public Set<String> keysStartingWith(String prefix) {
        if (redisEnabled) {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            return keys == null ? Set.of() : Set.copyOf(keys);
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        valueStore.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(keys::add);
        setStore.keySet().stream().filter(key -> key.startsWith(prefix)).forEach(keys::add);
        keys.forEach(this::evictIfExpired);
        return keys;
    }

    private void evictIfExpired(String key) {
        Instant expiresAt = expiresAtByKey.get(key);
        if (expiresAt == null || expiresAt.isAfter(Instant.now())) {
            return;
        }
        delete(key);
    }
}
