package com.youyu.backend.controller.chat;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.YouyuBackendApplication;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = YouyuBackendApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuickReplyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static Long firstQuickReplyId;
    private static Long secondQuickReplyId;

    @Test
    @Order(1)
    void createQuickReplySuccess() throws Exception {
        String response = mockMvc.perform(post("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Thanks for your message.", "sortOrder": 2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();

        firstQuickReplyId = ((Number) JsonPath.read(response, "$.data.id")).longValue();

        String secondResponse = mockMvc.perform(post("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Ships tomorrow.", "sortOrder": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        secondQuickReplyId = ((Number) JsonPath.read(secondResponse, "$.data.id")).longValue();
    }

    @Test
    @Order(2)
    void listQuickRepliesSortedBySortOrderAndCreatedAt() throws Exception {
        mockMvc.perform(get("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(secondQuickReplyId))
                .andExpect(jsonPath("$.data[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data[1].id").value(firstQuickReplyId))
                .andExpect(jsonPath("$.data[1].sortOrder").value(2));
    }

    @Test
    @Order(3)
    void updateQuickReplySuccess() throws Exception {
        mockMvc.perform(put("/api/chat/quick-replies/{id}", firstQuickReplyId)
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Updated reply.", "sortOrder": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(firstQuickReplyId))
                .andExpect(jsonPath("$.data[0].content").value("Updated reply."))
                .andExpect(jsonPath("$.data[0].sortOrder").value(0));
    }

    @Test
    @Order(4)
    void nonOwnerCannotUpdateOrDeleteQuickReply() throws Exception {
        mockMvc.perform(put("/api/chat/quick-replies/{id}", firstQuickReplyId)
                        .header("Authorization", "Bearer mock-1002-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Wrong owner.", "sortOrder": 0}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/chat/quick-replies/{id}", firstQuickReplyId)
                        .header("Authorization", "Bearer mock-1002-USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void contentValidationWorks() throws Exception {
        mockMvc.perform(post("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "   ", "sortOrder": 0}
                                """))
                .andExpect(status().isBadRequest());

        String longContent = "a".repeat(501);
        mockMvc.perform(post("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + longContent + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void deleteQuickReplySuccess() throws Exception {
        mockMvc.perform(delete("/api/chat/quick-replies/{id}", firstQuickReplyId)
                        .header("Authorization", "Bearer mock-1001-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(delete("/api/chat/quick-replies/{id}", secondQuickReplyId)
                        .header("Authorization", "Bearer mock-1001-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/quick-replies")
                        .header("Authorization", "Bearer mock-1001-USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
