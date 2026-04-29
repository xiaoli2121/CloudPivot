package io.cloudpivot.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class PlatformApiTest extends ApiIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldLoginWithPasswordAndReturnJwtPair() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.userName").value("Platform Admin"));
    }

    @Test
    void shouldAcceptLegacyUsernameFieldDuringCompatibilityWindow() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDisabledUserLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "analyst",
                                  "password": "analyst123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshTokenPair() throws Exception {
        String refreshToken = loginAndExtractRefreshToken("admin", "admin123");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString());
    }

    @Test
    void shouldLogoutAndInvalidateRefreshToken() throws Exception {
        String loginResponse = loginAndGetResponseBody("admin", "admin123");
        String accessToken = JsonPath.read(loginResponse, "$.data.accessToken");
        String refreshToken = JsonPath.read(loginResponse, "$.data.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "invalid-refresh-token"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldChangePasswordAndRejectOldPasswordAfterwards() throws Exception {
        String accessToken = loginAndExtractAccessToken("admin", "admin123");

        mockMvc.perform(put("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "admin123",
                                  "newPassword": "admin456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnCurrentUserContext() throws Exception {
        mockMvc.perform(get("/api/auth/current-user").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.roles[0]").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.data.orgName").value("CloudPivot Product Center"));
    }

    @Test
    void shouldRejectAnonymousAccessToProtectedApis() throws Exception {
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/iam/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/metadata/apps"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/current-user")
                        .header("Authorization", "Bearer definitely-invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExposeOpenApiDocsAndSwaggerUiAnonymously() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/swagger-ui/index.html")));
    }

    @Test
    void shouldReturnMenuTree() throws Exception {
        mockMvc.perform(get("/api/iam/menu-tree").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Dashboard"))
                .andExpect(jsonPath("$.data[1].children[0].path").value("/system/users"));
    }

    @Test
    void shouldReturnUsersAndRoles() throws Exception {
        mockMvc.perform(get("/api/iam/users").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].userName").value("Platform Admin"));

        mockMvc.perform(get("/api/iam/roles").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleCode").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.data[1].roleName").value("Implementation Consultant"));
    }

    @Test
    void shouldReturnSystemDictionariesAndAnnouncements() throws Exception {
        mockMvc.perform(get("/api/system/dictionaries").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dictCode").value("USER_STATUS"))
                .andExpect(jsonPath("$.data[0].items[0].label").value("Enabled"));

        mockMvc.perform(get("/api/system/announcements").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Community 1.0 Kickoff"))
                .andExpect(jsonPath("$.data[1].level").value("WARN"));
    }

    @Test
    void shouldReturnMetadataApps() throws Exception {
        mockMvc.perform(get("/api/metadata/apps").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].appCode").value("crm-leads"))
                .andExpect(jsonPath("$.data[1].status").value("PLANNING"));
    }

    @Test
    void shouldReadCurrentUserContextFromDatabase() throws Exception {
        jdbcTemplate.update("update iam_org set org_name = ? where id = ?", "DB_PLATFORM_CENTER", 1L);

        mockMvc.perform(get("/api/auth/current-user").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgName").value("DB_PLATFORM_CENTER"));
    }

    @Test
    void shouldReadMenuTreeFromDatabase() throws Exception {
        jdbcTemplate.update("update iam_menu set path = ? where menu_code = ?", "/users-db", "user-mgmt");

        mockMvc.perform(get("/api/iam/menu-tree").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].children[0].path").value("/users-db"));
    }

    @Test
    void shouldReadRolesFromDatabase() throws Exception {
        jdbcTemplate.update("update iam_role set role_name = ? where role_code = ?", "DB_IMPLEMENTATION_ROLE", "IMPLEMENTATION_CONSULTANT");

        mockMvc.perform(get("/api/iam/roles").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].roleName").value("DB_IMPLEMENTATION_ROLE"));
    }

    @Test
    void shouldReadSystemDataFromDatabase() throws Exception {
        jdbcTemplate.update("update sys_announcement set title = ? where id = ?", "DB_ANNOUNCEMENT", 1L);

        mockMvc.perform(get("/api/system/announcements").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("DB_ANNOUNCEMENT"));
    }

    @Test
    void shouldReadMetadataAppsFromDatabase() throws Exception {
        jdbcTemplate.update("update meta_app set owner_name = ? where app_code = ?", "DB_OWNER_TEAM", "crm-leads");

        mockMvc.perform(get("/api/metadata/apps").header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].owner").value("DB_OWNER_TEAM"));
    }

    private String bearerToken(String loginName, String password) throws Exception {
        return "Bearer " + loginAndExtractAccessToken(loginName, password);
    }

    private String loginAndExtractAccessToken(String loginName, String password) throws Exception {
        return JsonPath.read(loginAndGetResponseBody(loginName, password), "$.data.accessToken");
    }

    private String loginAndExtractRefreshToken(String loginName, String password) throws Exception {
        return JsonPath.read(loginAndGetResponseBody(loginName, password), "$.data.refreshToken");
    }

    private String loginAndGetResponseBody(String loginName, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "%s",
                                  "password": "%s"
                                }
                                """.formatted(loginName, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
