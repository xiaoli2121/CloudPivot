package io.cloudpivot.auth.security;

import java.util.List;

public record UserPrincipal(
        long userId,
        String userName,
        Long orgId,
        String orgName,
        List<String> roles,
        List<String> permissions,
        boolean superAdmin) {
}
