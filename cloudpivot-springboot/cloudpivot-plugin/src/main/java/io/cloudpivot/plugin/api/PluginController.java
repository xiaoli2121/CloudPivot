package io.cloudpivot.plugin.api;

import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cloudpivot.common.api.ApiResponse;
import io.cloudpivot.common.api.ForbiddenException;
import io.cloudpivot.common.api.PageResponse;
import io.cloudpivot.plugin.api.dto.PluginSummary;
import io.cloudpivot.plugin.service.PluginCommandService;
import io.cloudpivot.plugin.service.PluginRegistryService;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginRegistryService pluginRegistryService;
    private final PluginCommandService pluginCommandService;

    public PluginController(
            PluginRegistryService pluginRegistryService,
            PluginCommandService pluginCommandService) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginCommandService = pluginCommandService;
    }

    @GetMapping("/registry")
    public ApiResponse<List<PluginSummary>> registry() {
        require("api:plugins:get");
        return ApiResponse.success(pluginRegistryService.registry());
    }

    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> plugins() {
        require("api:plugins:get");
        return ApiResponse.success(pluginRegistryService.plugins());
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> pluginDetail(@PathVariable Long id) {
        require("api:plugins:get");
        return ApiResponse.success(pluginRegistryService.pluginDetail(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> request) {
        require("api:plugins:post");
        return ApiResponse.success(pluginCommandService.create(request, actorUserId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        require("api:plugins:put");
        return ApiResponse.success(pluginCommandService.update(id, request, actorUserId()));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        require("api:plugins:put");
        return ApiResponse.success(pluginCommandService.updateStatus(id, String.valueOf(request.get("statusCode")), actorUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        require("api:plugins:delete");
        pluginCommandService.delete(id, actorUserId());
        return ApiResponse.success(null);
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
