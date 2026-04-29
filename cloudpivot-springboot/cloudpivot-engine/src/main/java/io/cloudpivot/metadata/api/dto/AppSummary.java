package io.cloudpivot.metadata.api.dto;

public record AppSummary(
        long appId,
        String appCode,
        String appName,
        String owner,
        String status) {
}
