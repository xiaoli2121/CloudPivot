package io.cloudpivot.auth.api.dto;

public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
