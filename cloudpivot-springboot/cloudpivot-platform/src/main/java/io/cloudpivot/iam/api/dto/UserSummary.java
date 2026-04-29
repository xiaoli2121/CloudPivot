package io.cloudpivot.iam.api.dto;

import java.util.List;

public record UserSummary(
        long userId,
        String userName,
        String loginName,
        String orgName,
        String status,
        List<String> roles) {
}
