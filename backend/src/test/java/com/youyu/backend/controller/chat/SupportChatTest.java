package com.youyu.backend.controller.chat;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportChatTest extends BackendTestBase {

    private static final String USER = "mock-1001-USER";
    private static final String ADMIN = "mock-9001-ADMIN";
    private static final String SUPPORT_AGENT = "mock-9102-SUPPORT_AGENT";
    private static final String REVIEWER = "mock-9103-REVIEWER";

    @BeforeEach
    void cleanupSupportConversations() {
        jdbcTemplate.update("DELETE FROM chat_messages WHERE conversation_id IN (SELECT id FROM chat_conversations WHERE type = 'support')");
        jdbcTemplate.update("DELETE FROM chat_conversations WHERE type = 'support'");
    }

    private long startSession() throws Exception {
        String response = mockMvc.perform(post("/api/chat/support/session")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.type").value("support"))
                .andExpect(jsonPath("$.data.supportStatus").value("ai"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.id")).longValue();
    }

    @Test
    void startSupportSessionIsIdempotent() throws Exception {
        long first = startSession();
        String response = mockMvc.perform(post("/api/chat/support/session")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("support"))
                .andReturn().getResponse().getContentAsString();
        long second = ((Number) JsonPath.read(response, "$.data.id")).longValue();
        org.junit.jupiter.api.Assertions.assertEquals(first, second);
    }

    @Test
    void aiBotRepliesWithFaqAnswer() throws Exception {
        long conversationId = startSession();

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"我想申请退款，怎么操作？\"}"))
                .andExpect(status().isOk());

        // Newest message (DESC order) should be the platform CS (1099) auto-reply mentioning refunds.
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].senderUserId").value(1099))
                .andExpect(jsonPath("$.data.content[0].body", containsString("退款")));
    }

    @Test
    void escalationStopsAiAndSurfacesInAdminQueue() throws Exception {
        long conversationId = startSession();

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/escalate")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk());

        // After escalation a follow-up user message must NOT trigger an AI reply.
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"还是需要人工帮忙\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));

        // Conversation appears in the admin pending queue.
        mockMvc.perform(get("/api/admin/support/chat/conversations")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .param("filter", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].supportStatus").value("pending"));
    }

    @Test
    void adminCanClaimReplyAndClose() throws Exception {
        long conversationId = startSession();
        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/escalate")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/support/chat/conversations/" + conversationId + "/claim")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportStatus").value("human"))
                .andExpect(jsonPath("$.data.assignedAdminId").value(9001));

        mockMvc.perform(post("/api/admin/support/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"您好，已为您核查订单，请稍候。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderUserId").value(1099));

        // User can see the platform CS reply as the newest message.
        mockMvc.perform(get("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].senderUserId").value(1099))
                .andExpect(jsonPath("$.data.content[0].body", containsString("核查订单")));

        mockMvc.perform(post("/api/admin/support/chat/conversations/" + conversationId + "/close")
                        .header("Authorization", "Bearer " + ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportStatus").value("closed"));
    }

    @Test
    void userCanCloseAndRestartSupportSession() throws Exception {
        long conversationId = startSession();

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/close-support")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportStatus").value("closed"));

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"还想继续问\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("再次咨询")));

        mockMvc.perform(post("/api/chat/support/session")
                        .header("Authorization", "Bearer " + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value((int) conversationId))
                .andExpect(jsonPath("$.data.supportStatus").value("ai"));

        mockMvc.perform(post("/api/chat/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"重新咨询退款\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rolesWithoutPermissionCannotAccessConsole() throws Exception {
        mockMvc.perform(get("/api/admin/support/chat/conversations")
                        .header("Authorization", "Bearer " + REVIEWER)
                        .param("filter", "pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
