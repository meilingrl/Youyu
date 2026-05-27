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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(classes = YouyuBackendApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static Long conversationId;
    private static Long orderConversationId;
    private static Long recalledMessageId;

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
    void unreadCountAndMarkReadWork() throws Exception {
        String token = "mock-1001-USER";

        mockMvc.perform(get("/api/chat/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(conversationId))
                .andExpect(jsonPath("$.data.content[0].unreadCount").value(1));

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    @Order(15)
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

    @Test
    @Order(16)
    void sendProductCardMessageSuccess() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Please check this product", "messageType": "product_card", "productId": 3001}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageType").value("product_card"))
                .andExpect(jsonPath("$.data.productId").value(3001))
                .andExpect(jsonPath("$.data.product.id").value(3001))
                .andExpect(jsonPath("$.data.product.title").value("Advanced Math Review Pack"))
                .andExpect(jsonPath("$.data.product.price").value(19.90))
                .andExpect(jsonPath("$.data.product.status").value("on_sale"))
                .andExpect(jsonPath("$.data.product.imageUrl").exists());
    }

    @Test
    @Order(17)
    void getMessagesIncludesProductCardSummary() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].messageType").value("product_card"))
                .andExpect(jsonPath("$.data.content[0].productId").value(3001))
                .andExpect(jsonPath("$.data.content[0].product.id").value(3001))
                .andExpect(jsonPath("$.data.content[0].product.title").value("Advanced Math Review Pack"))
                .andExpect(jsonPath("$.data.content[0].product.price").value(19.90))
                .andExpect(jsonPath("$.data.content[0].product.status").value("on_sale"))
                .andExpect(jsonPath("$.data.content[0].product.imageUrl").exists());
    }

    @Test
    @Order(18)
    void sendProductCardRequiresExistingShareableProduct() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageType": "product_card", "productId": 999999}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageType": "product_card", "productId": 3004}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(19)
    void createOrderConversationSuccess() throws Exception {
        String token = "mock-1010-USER";
        String response = mockMvc.perform(post("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"peerUserId": 1004}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();

        orderConversationId = ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    @Test
    @Order(20)
    void sendOrderCardMessageSuccess() throws Exception {
        String token = "mock-1010-USER";
        mockMvc.perform(post("/api/chat/conversations/" + orderConversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Please check this order", "messageType": "order_card", "orderId": 8001}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messageType").value("order_card"))
                .andExpect(jsonPath("$.data.orderId").value(8001))
                .andExpect(jsonPath("$.data.order.id").value(8001))
                .andExpect(jsonPath("$.data.order.orderNumber").value("SEED8001"))
                .andExpect(jsonPath("$.data.order.status").value("pending_payment"))
                .andExpect(jsonPath("$.data.order.totalAmount").value(35.00))
                .andExpect(jsonPath("$.data.order.productTitle").value("Engineering Drawing Tool Set"))
                .andExpect(jsonPath("$.data.order.productImage").exists());
    }

    @Test
    @Order(21)
    void getMessagesIncludesOrderCardSummary() throws Exception {
        String token = "mock-1010-USER";
        mockMvc.perform(get("/api/chat/conversations/" + orderConversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].messageType").value("order_card"))
                .andExpect(jsonPath("$.data.content[0].orderId").value(8001))
                .andExpect(jsonPath("$.data.content[0].order.id").value(8001))
                .andExpect(jsonPath("$.data.content[0].order.orderNumber").value("SEED8001"))
                .andExpect(jsonPath("$.data.content[0].order.status").value("pending_payment"))
                .andExpect(jsonPath("$.data.content[0].order.totalAmount").value(35.00))
                .andExpect(jsonPath("$.data.content[0].order.productTitle").value("Engineering Drawing Tool Set"))
                .andExpect(jsonPath("$.data.content[0].order.productImage").exists());
    }

    @Test
    @Order(22)
    void sendOrderCardRequiresExistingParticipantOrder() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageType": "order_card", "orderId": 999999}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messageType": "order_card", "orderId": 8001}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(23)
    void searchMessagesFindsSeedConversationMessages() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Unique P2 search target"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/messages/search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "Unique P2 search target")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].body").value("Unique P2 search target"))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(24)
    void pinAndMuteConversationWork() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/pin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pinned": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/mute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"muted": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(conversationId));
    }

    @Test
    @Order(25)
    void recallOwnMessageWithinWindowWorks() throws Exception {
        String token = "mock-1001-USER";
        String response = mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Please keep this private for a minute"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        recalledMessageId = ((Number) JsonPath.read(response, "$.data.id")).longValue();

        mockMvc.perform(post("/api/chat/messages/" + recalledMessageId + "/recall")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(recalledMessageId))
                .andExpect(jsonPath("$.data.content[0].isRecalled").value(true));
    }

    @Test
    @Order(26)
    void autoReplySettingsAndTriggerWork() throws Exception {
        String sellerToken = "mock-1002-USER";
        mockMvc.perform(put("/api/chat/auto-reply")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isEnabled": true, "replyContent": "您好，我稍后查看后回复您"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/auto-reply")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isEnabled").value(true))
                .andExpect(jsonPath("$.data.replyContent").value("您好，我稍后查看后回复您"));

        String buyerToken = "mock-1001-USER";
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "请问这个周末还能自提吗？"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + buyerToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.senderUserId == 1002)].body", hasItem("您好，我稍后查看后回复您")));
    }

    @Test
    @Order(27)
    void deleteConversationRemovesItFromList() throws Exception {
        String token = "mock-1001-USER";
        mockMvc.perform(delete("/api/chat/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/chat/conversations")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(conversationId.intValue()))));
    }
}
