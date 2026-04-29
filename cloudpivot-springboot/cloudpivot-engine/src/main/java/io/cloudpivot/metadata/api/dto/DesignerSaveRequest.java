package io.cloudpivot.metadata.api.dto;

import java.util.List;
import java.util.Map;

public record DesignerSaveRequest(
        String pageName,
        String pageType,
        String routePath,
        String statusCode,
        List<ComponentDraft> components) {

    public record ComponentDraft(
            String componentCode,
            String componentType,
            String parentCode,
            Integer sortNo,
            Map<String, Object> props) {
    }
}
