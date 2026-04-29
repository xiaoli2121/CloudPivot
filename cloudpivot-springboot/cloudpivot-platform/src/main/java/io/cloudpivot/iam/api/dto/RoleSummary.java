package io.cloudpivot.iam.api.dto;

public record RoleSummary(
        long roleId,
        String roleCode,
        String roleName,
        String dataScope) {
}
