package io.cloudpivot.system.api;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cloudpivot.common.api.ApiResponse;
import io.cloudpivot.common.api.PageResponse;
import io.cloudpivot.iam.security.PermissionGuard;
import io.cloudpivot.system.api.dto.AnnouncementSummary;
import io.cloudpivot.system.api.dto.DictionarySummary;
import io.cloudpivot.system.service.SystemCommandService;
import io.cloudpivot.system.service.SystemQueryService;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemQueryService systemQueryService;
    private final SystemCommandService systemCommandService;
    private final PermissionGuard permissionGuard;

    public SystemController(
            SystemQueryService systemQueryService,
            SystemCommandService systemCommandService,
            PermissionGuard permissionGuard) {
        this.systemQueryService = systemQueryService;
        this.systemCommandService = systemCommandService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/dictionaries")
    public ApiResponse<List<DictionarySummary>> dictionaries() {
        permissionGuard.check("api:system/dicts:get");
        return ApiResponse.success(systemQueryService.dictionaries());
    }

    @GetMapping("/announcements")
    public ApiResponse<List<AnnouncementSummary>> announcements() {
        permissionGuard.check("api:system/announcements:get");
        return ApiResponse.success(systemQueryService.announcements());
    }

    @PostMapping("/dictionaries")
    public ApiResponse<Map<String, Object>> createDictionary(@RequestBody Map<String, Object> request) {
        permissionGuard.check("api:system/dicts:post");
        return ApiResponse.success(systemCommandService.createDictionary(request, permissionGuard.currentPrincipal()));
    }

    @PutMapping("/dictionaries/{id}")
    public ApiResponse<Map<String, Object>> updateDictionary(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:system/dicts:put");
        return ApiResponse.success(systemCommandService.updateDictionary(id, request, permissionGuard.currentPrincipal()));
    }

    @DeleteMapping("/dictionaries/{id}")
    public ApiResponse<Void> deleteDictionary(@PathVariable Long id) {
        permissionGuard.check("api:system/dicts:delete");
        systemCommandService.deleteDictionary(id, permissionGuard.currentPrincipal());
        return ApiResponse.success(null);
    }

    @GetMapping("/dictionaries/{dictId}/items")
    public ApiResponse<List<Map<String, Object>>> dictionaryItems(@PathVariable Long dictId) {
        permissionGuard.check("api:system/dicts:get");
        return ApiResponse.success(systemCommandService.dictionaryItems(dictId));
    }

    @PostMapping("/dictionaries/{dictId}/items")
    public ApiResponse<Map<String, Object>> createDictionaryItem(@PathVariable Long dictId, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:system/dicts:post");
        return ApiResponse.success(systemCommandService.createDictionaryItem(dictId, request, permissionGuard.currentPrincipal()));
    }

    @PutMapping("/dictionaries/{dictId}/items/{itemId}")
    public ApiResponse<Map<String, Object>> updateDictionaryItem(
            @PathVariable Long dictId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:system/dicts:put");
        return ApiResponse.success(systemCommandService.updateDictionaryItem(dictId, itemId, request, permissionGuard.currentPrincipal()));
    }

    @DeleteMapping("/dictionaries/{dictId}/items/{itemId}")
    public ApiResponse<Void> deleteDictionaryItem(@PathVariable Long dictId, @PathVariable Long itemId) {
        permissionGuard.check("api:system/dicts:delete");
        systemCommandService.deleteDictionaryItem(dictId, itemId, permissionGuard.currentPrincipal());
        return ApiResponse.success(null);
    }

    @PostMapping("/announcements")
    public ApiResponse<Map<String, Object>> createAnnouncement(@RequestBody Map<String, Object> request) {
        permissionGuard.check("api:system/announcements:post");
        return ApiResponse.success(systemCommandService.createAnnouncement(request, permissionGuard.currentPrincipal()));
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<Map<String, Object>> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:system/announcements:put");
        return ApiResponse.success(systemCommandService.updateAnnouncement(id, request, permissionGuard.currentPrincipal()));
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Void> deleteAnnouncement(@PathVariable Long id) {
        permissionGuard.check("api:system/announcements:delete");
        systemCommandService.deleteAnnouncement(id, permissionGuard.currentPrincipal());
        return ApiResponse.success(null);
    }

    @GetMapping("/login-logs")
    public ApiResponse<PageResponse<Map<String, Object>>> loginLogs() {
        permissionGuard.check("api:system/login-logs:get");
        return ApiResponse.success(systemCommandService.loginLogs());
    }
}
