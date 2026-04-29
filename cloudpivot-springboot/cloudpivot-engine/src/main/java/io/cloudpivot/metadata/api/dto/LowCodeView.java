package io.cloudpivot.metadata.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class LowCodeView {

    private LowCodeView() {
    }

    public record AppDetail(
            long appId,
            String appCode,
            String appName,
            String owner,
            String status) {
    }

    public record FieldDetail(
            long fieldId,
            String fieldCode,
            String fieldName,
            String fieldType,
            boolean required,
            int sortNo) {
    }

    public record ObjectDetail(
            long objectId,
            String objectCode,
            String objectName,
            String storeType,
            String primaryFieldCode,
            String statusCode,
            List<FieldDetail> fields) {
    }

    public record ComponentDetail(
            String componentCode,
            String componentType,
            String parentCode,
            Integer sortNo,
            Map<String, Object> props) {
    }

    public record PageDetail(
            long pageId,
            String pageCode,
            String pageName,
            String pageType,
            String routePath,
            String statusCode,
            List<ComponentDetail> components) {
    }

    public record PublishedVersion(
            long versionId,
            String versionCode,
            String versionStatus,
            String snapshotSummary,
            LocalDateTime publishedTime) {
    }
}
