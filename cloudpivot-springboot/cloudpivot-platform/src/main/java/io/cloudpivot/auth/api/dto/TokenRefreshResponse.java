package io.cloudpivot.auth.api.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresIn) {
}
