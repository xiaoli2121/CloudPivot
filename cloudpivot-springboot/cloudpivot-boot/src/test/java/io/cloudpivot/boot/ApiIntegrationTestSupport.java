package io.cloudpivot.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import io.cloudpivot.auth.service.RedisService;

abstract class ApiIntegrationTestSupport {

    @Autowired
    private RedisService redisService;

    @BeforeEach
    void clearCachedAuthState() {
        redisService.keysStartingWith("cp:").forEach(redisService::delete);
    }

    protected String bearerToken(MockMvc mockMvc, String loginName, String password) throws Exception {
        return "Bearer " + accessToken(mockMvc, loginName, password);
    }

    protected String accessToken(MockMvc mockMvc, String loginName, String password) throws Exception {
        return JsonPath.read(loginResponse(mockMvc, loginName, password), "$.data.accessToken");
    }

    protected String refreshToken(MockMvc mockMvc, String loginName, String password) throws Exception {
        return JsonPath.read(loginResponse(mockMvc, loginName, password), "$.data.refreshToken");
    }

    protected String loginResponse(MockMvc mockMvc, String loginName, String password) throws Exception {
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
