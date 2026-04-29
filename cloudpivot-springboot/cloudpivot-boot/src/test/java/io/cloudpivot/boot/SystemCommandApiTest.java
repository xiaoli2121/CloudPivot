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
class SystemCommandApiTest extends ApiIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUpdateAndDeleteDictionaryAndItems() throws Exception {
        String dictResponse = mockMvc.perform(post("/api/system/dictionaries")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictCode": "LEAD_STAGE",
                                  "dictName": "Lead Stage"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dictCode").value("LEAD_STAGE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long dictId = JsonPath.read(dictResponse, "$.data.id");

        mockMvc.perform(put("/api/system/dictionaries/{id}", dictId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictName": "Lead Pipeline Stage"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dictName").value("Lead Pipeline Stage"));

        String itemResponse = mockMvc.perform(post("/api/system/dictionaries/{dictId}/items", dictId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemLabel": "Qualified",
                                  "itemValue": "QUALIFIED",
                                  "sortNo": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemLabel").value("Qualified"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long itemId = JsonPath.read(itemResponse, "$.data.id");

        mockMvc.perform(put("/api/system/dictionaries/{dictId}/items/{itemId}", dictId, itemId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemLabel": "Qualified Lead",
                                  "itemValue": "QUALIFIED",
                                  "sortNo": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemLabel").value("Qualified Lead"))
                .andExpect(jsonPath("$.data.sortNo").value(2));

        mockMvc.perform(get("/api/system/dictionaries/{dictId}/items", dictId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemLabel").value("Qualified Lead"));

        mockMvc.perform(delete("/api/system/dictionaries/{dictId}/items/{itemId}", dictId, itemId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(delete("/api/system/dictionaries/{id}", dictId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        Integer deletedFlag = jdbcTemplate.queryForObject(
                "select deleted_flag from sys_dict where id = ?",
                Integer.class,
                dictId);
        assertThat(deletedFlag).isEqualTo(1);
    }

    @Test
    void shouldCreateUpdateAndDeleteAnnouncement() throws Exception {
        String response = mockMvc.perform(post("/api/system/announcements")
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Maintenance Window",
                                  "levelCode": "WARN",
                                  "publisherName": "Platform SRE",
                                  "publishTime": "2026-05-01 22:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Maintenance Window"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long announcementId = JsonPath.read(response, "$.data.id");

        mockMvc.perform(put("/api/system/announcements/{id}", announcementId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Maintenance Window Updated",
                                  "levelCode": "INFO",
                                  "publisherName": "Platform SRE",
                                  "publishTime": "2026-05-02 09:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Maintenance Window Updated"))
                .andExpect(jsonPath("$.data.level").value("INFO"));

        mockMvc.perform(delete("/api/system/announcements/{id}", announcementId)
                        .header("Authorization", bearerToken(mockMvc, "admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    void shouldReturnLoginLogs() throws Exception {
        String accessToken = accessToken(mockMvc, "admin", "admin123");

        mockMvc.perform(get("/api/system/login-logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].loginName").value("admin"))
                .andExpect(jsonPath("$.data.records[0].actionCode").value("LOGIN"));
    }

    @Test
    void shouldEnforcePermissionsOnSystemCommandApis() throws Exception {
        mockMvc.perform(post("/api/system/dictionaries")
                        .header("Authorization", bearerToken(mockMvc, "consultant", "consultant123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dictCode": "FORBIDDEN_DICT",
                                  "dictName": "Forbidden"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
