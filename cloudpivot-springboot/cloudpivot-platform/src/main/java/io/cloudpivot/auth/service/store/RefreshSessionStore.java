package io.cloudpivot.auth.service.store;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudpivot.auth.service.RedisService;

@Component
public class RefreshSessionStore {

    private static final String SESSION_KEY_PREFIX = "cp:refresh:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "cp:user:refresh-sessions:";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RefreshSessionStore(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public void save(RefreshSession session) {
        Duration ttl = Duration.between(Instant.now(), session.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisService.set(sessionKey(session.sessionId()), serialize(session), ttl);
        redisService.addMember(userSessionsKey(session.userId()), session.sessionId(), ttl);
    }

    public boolean matches(String sessionId, String refreshTokenId, long userId, long authVersion) {
        RefreshSession session = deserialize(redisService.get(sessionKey(sessionId)));
        if (session == null) {
            return false;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            delete(sessionId);
            return false;
        }
        return session.userId() == userId
                && session.authVersion() == authVersion
                && session.refreshTokenId().equals(refreshTokenId);
    }

    public void delete(String sessionId) {
        RefreshSession session = deserialize(redisService.get(sessionKey(sessionId)));
        if (session != null) {
            redisService.removeMember(userSessionsKey(session.userId()), sessionId);
        }
        redisService.delete(sessionKey(sessionId));
    }

    public void deleteByUserId(long userId) {
        for (String sessionId : redisService.members(userSessionsKey(userId))) {
            redisService.delete(sessionKey(sessionId));
        }
        redisService.delete(userSessionsKey(userId));
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionsKey(long userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }

    private String serialize(RefreshSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize refresh session.", exception);
        }
    }

    private RefreshSession deserialize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RefreshSession.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to deserialize refresh session.", exception);
        }
    }

    public record RefreshSession(
            String sessionId,
            String refreshTokenId,
            long userId,
            long authVersion,
            Instant expiresAt) {
    }
}
