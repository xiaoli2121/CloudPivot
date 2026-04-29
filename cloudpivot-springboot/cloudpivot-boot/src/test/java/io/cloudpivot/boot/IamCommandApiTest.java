package io.cloudpivot.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IamCommandApiTest extends ApiIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUpdateAndDeleteUser() throws Exception {
        String createResponse = mockMvc.perform(post("/api/iam/users")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "QA Engineer",
                                  "loginName": "qa-engineer",
                                  "password": "qa123456",
                                  "orgId": 2,
                                  "phone": "13800000001",
                                  "email": "qa@cloudpivot.io",
                                  "userStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginName").value("qa-engineer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(put("/api/iam/users/{id}", userId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "QA Lead",
                                  "orgId": 3,
                                  "phone": "13800000009",
                                  "email": "qa.lead@cloudpivot.io",
                                  "userStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("QA Lead"))
                .andExpect(jsonPath("$.data.orgName").value("Solution Center"));

        mockMvc.perform(delete("/api/iam/users/{id}", userId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        Integer deletedFlag = jdbcTemplate.queryForObject(
                "select deleted_flag from iam_user where id = ?",
                Integer.class,
                userId);
        assertThat(deletedFlag).isEqualTo(1);
    }

    @Test
    void shouldResetPasswordDisableUnlockAssignRolesAndForceLogout() throws Exception {
        String consultantRefreshToken = refreshToken(mockMvc, "consultant", "consultant123");

        mockMvc.perform(put("/api/iam/users/{id}/reset-password", 2L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "newPassword": "consultant456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "consultant",
                                  "password": "consultant123"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "consultant",
                                  "password": "consultant456"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/iam/users/{id}/status", 2L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userStatus": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value("DISABLED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "consultant",
                                  "password": "consultant456"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        jdbcTemplate.update("update iam_user set user_status = 'ENABLED', lock_expire_time = timestamp '2099-01-01 00:00:00' where id = ?", 2L);

        mockMvc.perform(put("/api/iam/users/{id}/unlock", 2L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(put("/api/iam/users/{id}/roles", 2L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleIds": [2, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("IMPLEMENTATION_CONSULTANT"))
                .andExpect(jsonPath("$.data.roles[1]").value("BUSINESS_ANALYST"));

        mockMvc.perform(post("/api/iam/users/{id}/force-logout", 2L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(consultantRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateUpdateAssignAndDeleteRole() throws Exception {
        String createResponse = mockMvc.perform(post("/api/iam/roles")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCode": "OPS_MANAGER",
                                  "roleName": "Ops Manager",
                                  "dataScope": "ORG"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("OPS_MANAGER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roleId = JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(put("/api/iam/roles/{id}", roleId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleName": "Operations Manager",
                                  "dataScope": "CUSTOM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleName").value("Operations Manager"))
                .andExpect(jsonPath("$.data.dataScope").value("CUSTOM"));

        mockMvc.perform(put("/api/iam/roles/{id}/permissions", roleId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissionCodes": [
                                    "api:plugins:get",
                                    "api:plugins:put"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissionCodes[0]").value("api:plugins:get"))
                .andExpect(jsonPath("$.data.permissionCodes[1]").value("api:plugins:put"));

        mockMvc.perform(put("/api/iam/roles/{id}/data-scope", roleId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataScope": "CUSTOM",
                                  "orgIds": [2, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataScope").value("CUSTOM"))
                .andExpect(jsonPath("$.data.orgIds[0]").value(2))
                .andExpect(jsonPath("$.data.orgIds[1]").value(3));

        mockMvc.perform(delete("/api/iam/roles/{id}", roleId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void shouldCreateUpdateAndDeleteOrgAndMenu() throws Exception {
        String orgResponse = mockMvc.perform(post("/api/iam/orgs")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgName": "North Delivery Center",
                                  "parentId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgName").value("North Delivery Center"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orgId = JsonPath.read(orgResponse, "$.data.id");

        mockMvc.perform(put("/api/iam/orgs/{id}", orgId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgName": "North Region Delivery Center",
                                  "parentId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgName").value("North Region Delivery Center"));

        String menuResponse = mockMvc.perform(post("/api/iam/menus")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "menuCode": "audit-center",
                                  "menuName": "Audit Center",
                                  "menuType": "MENU",
                                  "path": "/system/audit-center",
                                  "parentId": 2,
                                  "icon": "Search",
                                  "sortNo": 9,
                                  "visibleFlag": 1,
                                  "permissionCode": "menu:audit-center"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuCode").value("audit-center"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long menuId = JsonPath.read(menuResponse, "$.data.id");

        mockMvc.perform(put("/api/iam/menus/{id}", menuId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "menuName": "Security Audit Center",
                                  "menuType": "MENU",
                                  "path": "/system/security-audit",
                                  "parentId": 2,
                                  "icon": "Search",
                                  "sortNo": 10,
                                  "visibleFlag": 1,
                                  "permissionCode": "menu:audit-center"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menuName").value("Security Audit Center"));

        mockMvc.perform(get("/api/iam/menus/tree")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].children[?(@.code=='audit-center')].name").value("Security Audit Center"));

        mockMvc.perform(delete("/api/iam/menus/{id}", menuId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(delete("/api/iam/orgs/{id}", orgId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void shouldEnforcePermissionsAndDataScopeOnIamApis() throws Exception {
        mockMvc.perform(get("/api/iam/users")
                        .header("Authorization", bearerToken(mockMvc, "consultant", "consultant123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].loginName").value("consultant"));

        mockMvc.perform(post("/api/iam/users")
                        .header("Authorization", bearerToken(mockMvc, "consultant", "consultant123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userName": "Unauthorized User",
                                  "loginName": "unauthorized-user",
                                  "password": "123456",
                                  "orgId": 2,
                                  "userStatus": "ENABLED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
