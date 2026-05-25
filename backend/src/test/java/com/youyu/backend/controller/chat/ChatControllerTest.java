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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = YouyuBackendApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static Long conversationId;

    @Test
    @Order(1)
    void createConversationSuccess() throws Exception {
        String token = "mock-1001-USER";
        String response = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"peerUserId": 1002, "productId": 3001}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.type").value("product_inquiry"))
                .andExpect(jsonPath("$.data.productId").value(3001))
                .andExpect(jsonPath("$.data.peerUser.id").value(1002))
                .andExpect(jsonPath("$.data.peerUser.nickname").exists())
                .andReturn().getResponse().getContentAsString();

        conversationId = ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    @Test
    @Order(2)
    void createConversationIdempotent() throws Exception {
        String token = "mock-1001-USER";
        // Create the same conversation again - should return existing one
        String response = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"peerUserId": 1002, "productId": 3001}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(conversationId))
                .andExpect(jsonPath("$.data.type").value("product_inquiry"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @Order(3)
    void sendMessageSuccess() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "你好,请问这本书还在吗?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.senderUserId").value(1001))
                .andExpect(jsonPath("$.data.body").value("你好,请问这本书还在吗?"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @Order(4)
    void sendMessageEmptyBodyFails() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    void sendMessageTooLongFails() throws Exception {
        String token = "mock-1001-USER";
        String longBody = "a".repeat(2001);
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"" + longBody + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void getMessagesSuccess() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @Order(7)
    void getMessagesNonParticipantFails() throws Exception {
        String token = "mock-1003-USER"; // Different user, not a participant
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void sendMessageNonParticipantFails() throws Exception {
        String token = "mock-1003-USER"; // Different user, not a participant
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Unauthorized message"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    void getConversationsSuccess() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(get("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @Order(10)
    void createConversationWithShopId() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"peerUserId": 1002, "shopId": 4001}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("shop_inquiry"))
                .andExpect(jsonPath("$.data.shopId").value(4001));
    }

    @Test
    @Order(11)
    void createConversationDirectType() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"peerUserId": 1003}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("direct"));
    }

    @Test
    @Order(12)
    void createConversationWithSelfFails() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"peerUserId": 1001}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    void peerUserCanSendMessage() throws Exception {
        // Peer user (1002) sends a reply
        String token = "mock-1002-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "在的,书还在"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.senderUserId").value(1002));
    }

    @Test
    @Order(14)
    void paginationWorks() throws Exception {
        String token = "mock-1001-USER";
        // Request with size=1 to test pagination
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.size").value(1));
    }
}
