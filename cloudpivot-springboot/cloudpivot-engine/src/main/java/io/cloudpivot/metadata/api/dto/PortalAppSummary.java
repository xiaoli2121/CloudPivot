package io.cloudpivot.metadata.api.dto;

public record PortalAppSummary(
        String appCode,
        String appName,
        String owner,
        String entryRoute,
        String versionCode) {
}
