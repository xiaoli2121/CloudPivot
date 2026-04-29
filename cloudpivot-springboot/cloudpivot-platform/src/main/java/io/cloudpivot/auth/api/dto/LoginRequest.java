package io.cloudpivot.auth.api.dto;

public record LoginRequest(String loginName, String username, String password) {

    public String resolvedLoginName() {
        if (loginName != null && !loginName.isBlank()) {
            return loginName;
        }
        return username;
    }
}
