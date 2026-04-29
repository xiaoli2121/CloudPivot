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
class MetadataCommandApiTest extends ApiIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUpdateReadAndDeleteApp() throws Exception {
        String createResponse = mockMvc.perform(post("/api/metadata/apps")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appCode": "supply-chain",
                                  "appName": "Supply Chain",
                                  "ownerName": "Operations Team",
                                  "appStatus": "PLANNING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appCode").value("supply-chain"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String appCode = JsonPath.read(createResponse, "$.data.appCode");

        mockMvc.perform(get("/api/metadata/apps/{appCode}", appCode)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("Supply Chain"));

        mockMvc.perform(put("/api/metadata/apps/{appCode}", appCode)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appName": "Supply Chain Control Tower",
                                  "ownerName": "Operations Excellence",
                                  "appStatus": "ACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("Supply Chain Control Tower"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/metadata/apps/{appCode}", appCode)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void shouldCreateUpdateAndDeleteObjectsAndFields() throws Exception {
        String objectResponse = mockMvc.perform(post("/api/metadata/apps/crm-leads/objects")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "objectCode": "follow_record",
                                  "objectName": "Follow Record",
                                  "storeType": "RELATIONAL",
                                  "primaryFieldCode": "subject",
                                  "statusCode": "DRAFT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectCode").value("follow_record"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long objectId = JsonPath.read(objectResponse, "$.data.id");

        mockMvc.perform(get("/api/metadata/apps/crm-leads/objects")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.objectCode=='follow_record')].objectName").value("Follow Record"));

        mockMvc.perform(get("/api/metadata/objects/{id}", objectId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectCode").value("follow_record"));

        mockMvc.perform(put("/api/metadata/objects/{id}", objectId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "objectName": "Customer Follow Record",
                                  "storeType": "RELATIONAL",
                                  "primaryFieldCode": "subject",
                                  "statusCode": "PUBLISHED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectName").value("Customer Follow Record"))
                .andExpect(jsonPath("$.data.statusCode").value("PUBLISHED"));

        String fieldResponse = mockMvc.perform(post("/api/metadata/objects/{objectId}/fields", objectId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fieldCode": "subject",
                                  "fieldName": "Subject",
                                  "fieldType": "TEXT",
                                  "requiredFlag": true,
                                  "sortNo": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldCode").value("subject"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long fieldId = JsonPath.read(fieldResponse, "$.data.id");

        mockMvc.perform(put("/api/metadata/fields/{id}", fieldId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fieldName": "Follow Subject",
                                  "fieldType": "TEXT",
                                  "requiredFlag": true,
                                  "sortNo": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fieldName").value("Follow Subject"));

        mockMvc.perform(delete("/api/metadata/fields/{id}", fieldId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(delete("/api/metadata/objects/{id}", objectId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        Integer deletedFlag = jdbcTemplate.queryForObject(
                "select deleted_flag from meta_object where id = ?",
                Integer.class,
                objectId);
        assertThat(deletedFlag).isEqualTo(1);
    }

    @Test
    void shouldReturnPublishVersionsAndPreserveDesignerFlow() throws Exception {
        mockMvc.perform(get("/api/metadata/apps/crm-leads/publish-versions")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].versionCode").value("v1.0.0"));

        mockMvc.perform(get("/api/metadata/apps/crm-leads/designer")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.app.appCode").value("crm-leads"));
    }

    @Test
    void shouldEnforceMetadataPermissions() throws Exception {
        mockMvc.perform(delete("/api/metadata/apps/{appCode}", "crm-leads")
                        .header("Authorization", bearerToken(mockMvc, "consultant", "consultant123")))
                .andExpect(status().isForbidden());
    }
}
