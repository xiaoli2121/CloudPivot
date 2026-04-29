package io.cloudpivot.metadata.api;

import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cloudpivot.common.api.ApiResponse;
import io.cloudpivot.common.api.ForbiddenException;
import io.cloudpivot.metadata.api.dto.AppSummary;
import io.cloudpivot.metadata.api.dto.DesignerSaveRequest;
import io.cloudpivot.metadata.api.dto.DesignerSaveResponse;
import io.cloudpivot.metadata.api.dto.DesignerSchemaResponse;
import io.cloudpivot.metadata.api.dto.PublishVersionRequest;
import io.cloudpivot.metadata.api.dto.PublishVersionResponse;
import io.cloudpivot.metadata.service.MetadataCommandService;
import io.cloudpivot.metadata.service.MetadataQueryService;
import io.cloudpivot.metadata.service.MetadataStudioService;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataQueryService metadataQueryService;
    private final MetadataStudioService metadataStudioService;
    private final MetadataCommandService metadataCommandService;

    public MetadataController(
            MetadataQueryService metadataQueryService,
            MetadataStudioService metadataStudioService,
            MetadataCommandService metadataCommandService) {
        this.metadataQueryService = metadataQueryService;
        this.metadataStudioService = metadataStudioService;
        this.metadataCommandService = metadataCommandService;
    }

    @GetMapping("/apps")
    public ApiResponse<List<AppSummary>> apps() {
        require("api:metadata/apps:get");
        return ApiResponse.success(metadataQueryService.apps());
    }

    @GetMapping("/apps/{appCode}")
    public ApiResponse<Map<String, Object>> appDetail(@PathVariable String appCode) {
        require("api:metadata/apps:get");
        return ApiResponse.success(metadataCommandService.appDetail(appCode));
    }

    @PostMapping("/apps")
    public ApiResponse<Map<String, Object>> createApp(@RequestBody Map<String, Object> request) {
        require("api:metadata/apps:post");
        return ApiResponse.success(metadataCommandService.createApp(request, actorUserId()));
    }

    @PutMapping("/apps/{appCode}")
    public ApiResponse<Map<String, Object>> updateApp(@PathVariable String appCode, @RequestBody Map<String, Object> request) {
        require("api:metadata/apps:put");
        return ApiResponse.success(metadataCommandService.updateApp(appCode, request, actorUserId()));
    }

    @DeleteMapping("/apps/{appCode}")
    public ApiResponse<Void> deleteApp(@PathVariable String appCode) {
        require("api:metadata/apps:delete");
        metadataCommandService.deleteApp(appCode, actorUserId());
        return ApiResponse.success(null);
    }

    @GetMapping("/apps/{appCode}/objects")
    public ApiResponse<List<Map<String, Object>>> objects(@PathVariable String appCode) {
        require("api:metadata/objects:get");
        return ApiResponse.success(metadataCommandService.objectsByApp(appCode));
    }

    @GetMapping("/objects/{id}")
    public ApiResponse<Map<String, Object>> objectDetail(@PathVariable Long id) {
        require("api:metadata/objects:get");
        return ApiResponse.success(metadataCommandService.objectDetail(id));
    }

    @PostMapping("/apps/{appCode}/objects")
    public ApiResponse<Map<String, Object>> createObject(@PathVariable String appCode, @RequestBody Map<String, Object> request) {
        require("api:metadata/objects:post");
        return ApiResponse.success(metadataCommandService.createObject(appCode, request, actorUserId()));
    }

    @PutMapping("/objects/{id}")
    public ApiResponse<Map<String, Object>> updateObject(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        require("api:metadata/objects:put");
        return ApiResponse.success(metadataCommandService.updateObject(id, request, actorUserId()));
    }

    @DeleteMapping("/objects/{id}")
    public ApiResponse<Void> deleteObject(@PathVariable Long id) {
        require("api:metadata/objects:delete");
        metadataCommandService.deleteObject(id, actorUserId());
        return ApiResponse.success(null);
    }

    @PostMapping("/objects/{objectId}/fields")
    public ApiResponse<Map<String, Object>> createField(@PathVariable Long objectId, @RequestBody Map<String, Object> request) {
        require("api:metadata/fields:post");
        return ApiResponse.success(metadataCommandService.createField(objectId, request, actorUserId()));
    }

    @PutMapping("/fields/{id}")
    public ApiResponse<Map<String, Object>> updateField(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        require("api:metadata/fields:put");
        return ApiResponse.success(metadataCommandService.updateField(id, request, actorUserId()));
    }

    @DeleteMapping("/fields/{id}")
    public ApiResponse<Void> deleteField(@PathVariable Long id) {
        require("api:metadata/fields:delete");
        metadataCommandService.deleteField(id, actorUserId());
        return ApiResponse.success(null);
    }

    @GetMapping("/apps/{appCode}/designer")
    public ApiResponse<DesignerSchemaResponse> designer(@PathVariable String appCode) {
        require("api:metadata/designer:get");
        return ApiResponse.success(metadataStudioService.designerSchema(appCode));
    }

    @PutMapping("/apps/{appCode}/designer")
    public ApiResponse<DesignerSaveResponse> saveDesigner(
            @PathVariable String appCode,
            @RequestBody DesignerSaveRequest request) {
        require("api:metadata/designer:put");
        return ApiResponse.success(metadataStudioService.saveDesigner(appCode, request));
    }

    @PostMapping("/apps/{appCode}/publish")
    public ApiResponse<PublishVersionResponse> publish(
            @PathVariable String appCode,
            @RequestBody PublishVersionRequest request) {
        require("api:metadata/publish:post");
        return ApiResponse.success(metadataStudioService.publish(appCode, request));
    }

    @GetMapping("/apps/{appCode}/publish-versions")
    public ApiResponse<List<Map<String, Object>>> publishVersions(@PathVariable String appCode) {
        require("api:metadata/publish:get");
        return ApiResponse.success(metadataCommandService.publishVersions(appCode));
    }

    private void require(String permissionCode) {
        if (!SecurityUtils.getSubject().isPermitted(permissionCode)) {
            throw new ForbiddenException("Permission denied: " + permissionCode);
        }
    }

    private long actorUserId() {
        return 1L;
    }
}
