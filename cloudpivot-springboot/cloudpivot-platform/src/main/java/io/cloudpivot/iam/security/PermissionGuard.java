package io.cloudpivot.iam.security;

import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;

import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.common.api.ForbiddenException;

@Component
public class PermissionGuard {

    public void check(String permissionCode) {
        UserPrincipal principal = currentPrincipal();
        if (principal.superAdmin()) {
            return;
        }
        if (!principal.permissions().contains(permissionCode)) {
            throw new ForbiddenException("Permission denied: " + permissionCode);
        }
    }

    public UserPrincipal currentPrincipal() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }
        throw new ForbiddenException("Authenticated principal is unavailable.");
    }
}
