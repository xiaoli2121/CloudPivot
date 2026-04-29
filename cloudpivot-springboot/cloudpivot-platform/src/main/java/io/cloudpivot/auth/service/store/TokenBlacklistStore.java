package io.cloudpivot.auth.service.store;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import io.cloudpivot.auth.service.RedisService;

@Component
public class TokenBlacklistStore {

    private static final String KEY_PREFIX = "cp:token:blacklist:";

    private final RedisService redisService;

    public TokenBlacklistStore(RedisService redisService) {
        this.redisService = redisService;
    }

    public void block(String tokenId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisService.set(KEY_PREFIX + tokenId, "1", ttl);
    }

    public boolean isBlocked(String tokenId) {
        return redisService.exists(KEY_PREFIX + tokenId);
    }
}
