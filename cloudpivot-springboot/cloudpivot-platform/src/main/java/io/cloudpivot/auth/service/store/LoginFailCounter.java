package io.cloudpivot.auth.service.store;

import java.time.Duration;
import org.springframework.stereotype.Component;

import io.cloudpivot.auth.service.RedisService;

@Component
public class LoginFailCounter {

    private static final String KEY_PREFIX = "cp:auth:fail:";

    private final RedisService redisService;

    public LoginFailCounter(RedisService redisService) {
        this.redisService = redisService;
    }

    public long increment(String loginName, Duration ttl) {
        return redisService.increment(KEY_PREFIX + loginName, ttl);
    }

    public void clear(String loginName) {
        redisService.delete(KEY_PREFIX + loginName);
    }
}
