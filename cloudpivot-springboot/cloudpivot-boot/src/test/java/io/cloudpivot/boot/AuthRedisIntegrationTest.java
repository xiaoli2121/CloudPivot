package io.cloudpivot.boot;

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
class AuthRedisIntegrationTest extends ApiIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRotateRefreshTokensAndRejectOldRefreshToken() throws Exception {
        String initialRefreshToken = refreshToken(mockMvc, "admin", "admin123");

        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(initialRefreshToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String rotatedRefreshToken = JsonPath.read(refreshResponse, "$.data.refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(initialRefreshToken)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void shouldTemporarilyLockUserAfterRepeatedFailuresAndAllowAdminUnlock() throws Exception {
        for (int index = 0; index < 5; index++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "loginName": "consultant",
                                      "password": "wrong-password"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "consultant",
                                  "password": "consultant123"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/iam/users/{id}/unlock", 2L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userName").value("Implementation Consultant"));
    }

    @Test
    void shouldWriteAuditLogsAndInvalidateCurrentAccessTokenAfterPasswordChange() throws Exception {
        String loginResponse = loginResponse(mockMvc, "admin", "admin123");
        String accessToken = JsonPath.read(loginResponse, "$.data.accessToken");
        String refreshToken = JsonPath.read(loginResponse, "$.data.refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "admin123",
                                  "newPassword": "admin789"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/current-user")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        Integer auditCount = jdbcTemplate.queryForObject("""
                select count(*)
                from sys_login_log
                where login_name = 'admin'
                  and action_code in ('LOGIN', 'REFRESH', 'CHANGE_PASSWORD')
                """, Integer.class);
        org.assertj.core.api.Assertions.assertThat(auditCount).isGreaterThanOrEqualTo(3);
    }
}
