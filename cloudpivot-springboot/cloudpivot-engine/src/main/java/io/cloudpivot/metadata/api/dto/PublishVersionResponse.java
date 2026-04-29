package io.cloudpivot.metadata.api.dto;

public record PublishVersionResponse(
        String appCode,
        String versionCode,
        String versionStatus,
        String snapshotSummary) {
}
