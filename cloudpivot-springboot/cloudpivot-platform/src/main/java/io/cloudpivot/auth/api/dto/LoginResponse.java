package io.cloudpivot.auth.api.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long userId,
        String userName,
        List<String> roles) {
}
