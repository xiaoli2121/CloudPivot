package io.cloudpivot.metadata.api.dto;

public record DesignerSaveResponse(
        String appCode,
        String pageName,
        String routePath,
        int componentCount) {
}
