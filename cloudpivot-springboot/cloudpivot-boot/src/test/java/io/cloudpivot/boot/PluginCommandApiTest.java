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
class PluginCommandApiTest extends ApiIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldListReadCreateUpdateToggleAndDeletePlugins() throws Exception {
        mockMvc.perform(get("/api/plugins")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.records[0].pluginCode").value("core-table"));

        mockMvc.perform(get("/api/plugins/{id}", 1L)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pluginCode").value("core-table"));

        String createResponse = mockMvc.perform(post("/api/plugins")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pluginCode": "approval-widget",
                                  "pluginName": "Approval Widget",
                                  "pluginType": "COMPONENT",
                                  "pluginVersion": "1.0.0",
                                  "statusCode": "ACTIVE",
                                  "entryPoint": "plugin://approval/widget",
                                  "description": "Approval workflow widget."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pluginCode").value("approval-widget"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long pluginId = JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(put("/api/plugins/{id}", pluginId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pluginName": "Approval Widget Pro",
                                  "pluginType": "COMPONENT",
                                  "pluginVersion": "1.1.0",
                                  "entryPoint": "plugin://approval/widget-pro",
                                  "description": "Approval workflow widget with escalation."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pluginName").value("Approval Widget Pro"))
                .andExpect(jsonPath("$.data.pluginVersion").value("1.1.0"));

        mockMvc.perform(put("/api/plugins/{id}/status", pluginId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusCode": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(delete("/api/plugins/{id}", pluginId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        Integer deletedFlag = jdbcTemplate.queryForObject(
                "select deleted_flag from plugin_registry where id = ?",
                Integer.class,
                pluginId);
        assertThat(deletedFlag).isEqualTo(1);
    }

    @Test
    void shouldKeepRegistryEndpointCompatible() throws Exception {
        mockMvc.perform(get("/api/plugins/registry")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pluginCode").value("core-table"))
                .andExpect(jsonPath("$.data[1].pluginCode").value("core-form"));
    }

    @Test
    void shouldEnforcePluginPermissions() throws Exception {
        mockMvc.perform(post("/api/plugins")
                        .header("Authorization", bearerToken(mockMvc, "consultant", "consultant123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pluginCode": "forbidden-plugin",
                                  "pluginName": "Forbidden Plugin",
                                  "pluginType": "COMPONENT",
                                  "pluginVersion": "1.0.0",
                                  "statusCode": "ACTIVE",
                                  "entryPoint": "plugin://forbidden",
                                  "description": "Should be rejected."
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
