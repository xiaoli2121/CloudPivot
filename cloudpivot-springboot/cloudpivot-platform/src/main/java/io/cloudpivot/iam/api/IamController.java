package io.cloudpivot.iam.api;

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

import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.common.api.ApiResponse;
import io.cloudpivot.common.api.PageResponse;
import io.cloudpivot.iam.api.dto.MenuNode;
import io.cloudpivot.iam.api.dto.RoleSummary;
import io.cloudpivot.iam.api.dto.UserSummary;
import io.cloudpivot.iam.security.PermissionGuard;
import io.cloudpivot.iam.service.IamCommandService;
import io.cloudpivot.iam.service.IamQueryService;

@RestController
@RequestMapping("/api/iam")
public class IamController {

    private final IamQueryService iamQueryService;
    private final IamCommandService iamCommandService;
    private final PermissionGuard permissionGuard;

    public IamController(
            IamQueryService iamQueryService,
            IamCommandService iamCommandService,
            PermissionGuard permissionGuard) {
        this.iamQueryService = iamQueryService;
        this.iamCommandService = iamCommandService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/menu-tree")
    public ApiResponse<List<MenuNode>> menuTree() {
        permissionGuard.check("api:iam/menus:get");
        return ApiResponse.success(iamQueryService.menuTree());
    }

    @GetMapping("/menus/tree")
    public ApiResponse<List<MenuNode>> menusTree() {
        permissionGuard.check("api:iam/menus:get");
        return ApiResponse.success(iamQueryService.menuTree());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserSummary>> users() {
        permissionGuard.check("api:iam/users:get");
        return ApiResponse.success(iamQueryService.users(permissionGuard.currentPrincipal()));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleSummary>> roles() {
        permissionGuard.check("api:iam/roles:get");
        return ApiResponse.success(iamQueryService.roles());
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/users:post");
        return ApiResponse.success(iamCommandService.createUser(request, actor()));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/users:put");
        return ApiResponse.success(iamCommandService.updateUser(id, request, actor()));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        permissionGuard.check("api:iam/users:delete");
        iamCommandService.deleteUser(id, actor());
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/users:put");
        iamCommandService.resetPassword(id, String.valueOf(request.get("newPassword")), actor());
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/users:put");
        return ApiResponse.success(iamCommandService.updateUserStatus(id, String.valueOf(request.get("userStatus")), actor()));
    }

    @PutMapping("/users/{id}/unlock")
    public ApiResponse<Void> unlockUser(@PathVariable Long id) {
        permissionGuard.check("api:iam/users:put");
        iamCommandService.unlockUser(id, actor());
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{id}/roles")
    public ApiResponse<Map<String, Object>> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> request) {
        permissionGuard.check("api:iam/users:put");
        return ApiResponse.success(iamCommandService.assignUserRoles(id, request.getOrDefault("roleIds", List.of()), actor()));
    }

    @PostMapping("/users/{id}/force-logout")
    public ApiResponse<Void> forceLogout(@PathVariable Long id) {
        permissionGuard.check("api:iam/users:post");
        iamCommandService.forceLogout(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/roles")
    public ApiResponse<Map<String, Object>> createRole(@RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/roles:post");
        return ApiResponse.success(iamCommandService.createRole(request, actor()));
    }

    @PutMapping("/roles/{id}")
    public ApiResponse<Map<String, Object>> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/roles:put");
        return ApiResponse.success(iamCommandService.updateRole(id, request, actor()));
    }

    @PutMapping("/roles/{id}/permissions")
    public ApiResponse<Map<String, Object>> assignRolePermissions(@PathVariable Long id, @RequestBody Map<String, List<String>> request) {
        permissionGuard.check("api:iam/roles:put");
        return ApiResponse.success(iamCommandService.assignRolePermissions(id, request.getOrDefault("permissionCodes", List.of()), actor()));
    }

    @PutMapping("/roles/{id}/data-scope")
    public ApiResponse<Map<String, Object>> assignRoleDataScope(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/roles:put");
        @SuppressWarnings("unchecked")
        List<Integer> orgIdsRaw = (List<Integer>) request.getOrDefault("orgIds", List.of());
        List<Long> orgIds = orgIdsRaw.stream().map(Integer::longValue).toList();
        return ApiResponse.success(iamCommandService.assignRoleDataScope(id, String.valueOf(request.get("dataScope")), orgIds, actor()));
    }

    @DeleteMapping("/roles/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        permissionGuard.check("api:iam/roles:delete");
        iamCommandService.deleteRole(id, actor());
        return ApiResponse.success(null);
    }

    @PostMapping("/orgs")
    public ApiResponse<Map<String, Object>> createOrg(@RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/orgs:post");
        return ApiResponse.success(iamCommandService.createOrg(request, actor()));
    }

    @PutMapping("/orgs/{id}")
    public ApiResponse<Map<String, Object>> updateOrg(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/orgs:put");
        return ApiResponse.success(iamCommandService.updateOrg(id, request, actor()));
    }

    @DeleteMapping("/orgs/{id}")
    public ApiResponse<Void> deleteOrg(@PathVariable Long id) {
        permissionGuard.check("api:iam/orgs:delete");
        iamCommandService.deleteOrg(id, actor());
        return ApiResponse.success(null);
    }

    @PostMapping("/menus")
    public ApiResponse<Map<String, Object>> createMenu(@RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/menus:post");
        return ApiResponse.success(iamCommandService.createMenu(request, actor()));
    }

    @PutMapping("/menus/{id}")
    public ApiResponse<Map<String, Object>> updateMenu(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        permissionGuard.check("api:iam/menus:put");
        return ApiResponse.success(iamCommandService.updateMenu(id, request, actor()));
    }

    @DeleteMapping("/menus/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        permissionGuard.check("api:iam/menus:delete");
        iamCommandService.deleteMenu(id, actor());
        return ApiResponse.success(null);
    }

    private UserPrincipal actor() {
        return permissionGuard.currentPrincipal();
    }
}
