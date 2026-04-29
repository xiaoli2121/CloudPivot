package io.cloudpivot.metadata.api.dto;

import java.util.List;

public record RuntimeEntryResponse(
        String appCode,
        String appName,
        LowCodeView.PageDetail page,
        List<LowCodeView.ComponentDetail> components,
        LowCodeView.PublishedVersion publishedVersion) {
}
