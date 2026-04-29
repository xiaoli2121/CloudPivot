package io.cloudpivot.auth.api.dto;

import java.util.List;

public record CurrentUserResponse(
        long userId,
        String userName,
        String orgName,
        List<String> roles,
        List<String> permissions) {
}
