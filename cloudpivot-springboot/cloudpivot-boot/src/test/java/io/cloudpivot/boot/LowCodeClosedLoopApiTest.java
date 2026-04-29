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
class LowCodeClosedLoopApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnDesignerSchemaForApplication() throws Exception {
        mockMvc.perform(get("/api/metadata/apps/crm-leads/designer")
                        .header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.app.appCode").value("crm-leads"))
                .andExpect(jsonPath("$.data.object.objectCode").value("lead"))
                .andExpect(jsonPath("$.data.page.pageCode").value("lead-list"))
                .andExpect(jsonPath("$.data.page.components[0].componentType").value("PAGE_CONTAINER"))
                .andExpect(jsonPath("$.data.latestPublishedVersion.versionCode").value("v1.0.0"));
    }

    @Test
    void shouldSaveDesignerSchemaIntoDraftTables() throws Exception {
        mockMvc.perform(put("/api/metadata/apps/crm-leads/designer")
                        .header("Authorization", bearerToken("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pageName": "Lead Pipeline",
                                  "pageType": "LIST",
                                  "routePath": "/crm/pipeline",
                                  "statusCode": "DRAFT",
                                  "components": [
                                    {
                                      "componentCode": "page-root",
                                      "componentType": "PAGE_CONTAINER",
                                      "sortNo": 1,
                                      "props": {
                                        "title": "Lead Pipeline"
                                      }
                                    },
                                    {
                                      "componentCode": "pipeline-board",
                                      "componentType": "TABLE",
                                      "parentCode": "page-root",
                                      "sortNo": 2,
                                      "props": {
                                        "columns": [
                                          "lead_name",
                                          "customer_name"
                                        ]
                                      }
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.pageName").value("Lead Pipeline"))
                .andExpect(jsonPath("$.data.routePath").value("/crm/pipeline"))
                .andExpect(jsonPath("$.data.componentCount").value(2));

        String pageName = jdbcTemplate.queryForObject(
                "select page_name from meta_page where app_id = ?",
                String.class,
                1L);
        Integer componentCount = jdbcTemplate.queryForObject(
                "select count(*) from meta_component where page_id = ?",
                Integer.class,
                1L);

        org.junit.jupiter.api.Assertions.assertEquals("Lead Pipeline", pageName);
        org.junit.jupiter.api.Assertions.assertEquals(2, componentCount);
    }

    @Test
    void shouldPublishRuntimeSnapshotForApplication() throws Exception {
        mockMvc.perform(post("/api/metadata/apps/crm-leads/publish")
                        .header("Authorization", bearerToken("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "versionNote": "Lead pipeline release"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.appCode").value("crm-leads"))
                .andExpect(jsonPath("$.data.versionCode").value("v1.0.1"))
                .andExpect(jsonPath("$.data.versionStatus").value("PUBLISHED"));

        Integer publishCount = jdbcTemplate.queryForObject(
                "select count(*) from meta_publish_version where app_id = ?",
                Integer.class,
                1L);

        org.junit.jupiter.api.Assertions.assertEquals(2, publishCount);
    }

    @Test
    void shouldReturnRuntimeEntryWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/runtime/apps/crm-leads/entry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.appCode").value("crm-leads"))
                .andExpect(jsonPath("$.data.page.pageCode").value("lead-list"))
                .andExpect(jsonPath("$.data.components[0].componentType").value("PAGE_CONTAINER"))
                .andExpect(jsonPath("$.data.publishedVersion.versionCode").value("v1.0.0"));
    }

    @Test
    void shouldReturnPortalApplicationListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/portal/apps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].appCode").value("crm-leads"))
                .andExpect(jsonPath("$.data[0].entryRoute").value("/crm/leads"));
    }

    @Test
    void shouldReturnRegisteredPlugins() throws Exception {
        mockMvc.perform(get("/api/plugins/registry")
                        .header("Authorization", bearerToken("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].pluginCode").value("core-table"))
                .andExpect(jsonPath("$.data[1].status").value("ACTIVE"));
    }

    private String bearerToken(String loginName, String password) throws Exception {
        return "Bearer " + JsonPath.read(loginAndGetResponseBody(loginName, password), "$.data.accessToken");
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
