package io.cloudpivot.metadata.api.dto;

public record DesignerSchemaResponse(
        LowCodeView.AppDetail app,
        LowCodeView.ObjectDetail object,
        LowCodeView.PageDetail page,
        LowCodeView.PublishedVersion latestPublishedVersion) {
}
