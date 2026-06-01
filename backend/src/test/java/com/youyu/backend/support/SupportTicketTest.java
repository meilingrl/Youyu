package com.youyu.backend.support;

import com.jayway.jsonpath.JsonPath;
import com.youyu.backend.BackendTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportTicketTest extends BackendTestBase {

    private static final String USER_ONE = "mock-1001-USER";
    private static final String USER_TWO = "mock-1002-USER";
    private static final String ADMIN = "mock-9001-ADMIN";
    private static final String SUPPORT_AGENT = "mock-9102-SUPPORT_AGENT";
    private static final String REVIEWER = "mock-9103-REVIEWER";

    @BeforeEach
    void resetSupportFixtures() {
        jdbcTemplate.update("DELETE FROM support_ticket_messages WHERE ticket_id IN (SELECT id FROM support_tickets WHERE ticket_no LIKE 'TST-%' OR ticket_no LIKE 'SUP-%')");
        jdbcTemplate.update("DELETE FROM support_tickets WHERE ticket_no LIKE 'TST-%' OR ticket_no LIKE 'SUP-%'");
        jdbcTemplate.update("DELETE FROM notifications WHERE user_id IN (1001, 1002)");
    }

    @Test
    void userCanCreateListDetailAndReplyToOwnTicket() throws Exception {
        Number ticketId = createTicket(USER_ONE, "order", "Order pickup help");

        mockMvc.perform(get("/api/support/tickets")
                        .header("Authorization", "Bearer " + USER_ONE)
                        .param("status", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].requesterUserId").value(1001));

        mockMvc.perform(get("/api/support/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + USER_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.id").value(ticketId))
                .andExpect(jsonPath("$.data.messages[0].messageType").value("public_reply"));

        mockMvc.perform(post("/api/support/tickets/{ticketId}/messages", ticketId)
                        .header("Authorization", "Bearer " + USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"I can provide the order number.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.ticket.lastRepliedBy").value("user"));
    }

    @Test
    void usersCannotReadOrReplyToOtherUsersTickets() throws Exception {
        Number ticketId = createTicket(USER_ONE, "account", "Account issue");

        mockMvc.perform(get("/api/support/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + USER_TWO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/support/tickets/{ticketId}/messages", ticketId)
                        .header("Authorization", "Bearer " + USER_TWO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"I should not access this.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanListDetailReplyAndAddInternalNote() throws Exception {
        Number ticketId = createTicket(USER_ONE, "payment", "Payment callback issue");

        mockMvc.perform(get("/api/admin/support/tickets")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .param("category", "payment")
                        .param("keyword", "callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/admin/support/tickets/{ticketId}/messages", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"public_reply\",\"content\":\"We are checking the payment record.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.lastRepliedBy").value("admin"));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + USER_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("support_ticket"))
                .andExpect(jsonPath("$.data.content[0].actionUrl").value("/app/support?ticketId=" + ticketId));

        mockMvc.perform(post("/api/admin/support/tickets/{ticketId}/messages", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"internal_note\",\"content\":\"Check gateway logs before replying.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[*].messageType", hasItem("internal_note")));

        mockMvc.perform(get("/api/admin/support/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + SUPPORT_AGENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[*].messageType", hasItem("internal_note")));

        mockMvc.perform(get("/api/support/tickets/{ticketId}", ticketId)
                        .header("Authorization", "Bearer " + USER_ONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[*].messageType", not(hasItem("internal_note"))));
    }

    @Test
    void adminStatusTransitionsAndPermissionsAreEnforced() throws Exception {
        Number ticketId = createTicket(USER_ONE, "shop", "Shop review question");

        mockMvc.perform(get("/api/admin/support/tickets")
                        .header("Authorization", "Bearer " + REVIEWER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/admin/support/tickets/{ticketId}/status", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"waiting_user\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(put("/api/admin/support/tickets/{ticketId}/status", ticketId)
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in_progress\",\"assignToMe\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.status").value("in_progress"))
                .andExpect(jsonPath("$.data.ticket.assignedAdminUserId").value(9102));

        mockMvc.perform(get("/api/admin/support/tickets")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .param("assignedToMe", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].assignedAdminUserId").value(9102));

        mockMvc.perform(put("/api/admin/support/tickets/{ticketId}/status", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"waiting_user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.status").value("waiting_user"));

        mockMvc.perform(put("/api/admin/support/tickets/{ticketId}/status", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.status").value("resolved"));
    }

    @Test
    void closedTicketsRejectUserAndAdminReplies() throws Exception {
        Number ticketId = createTicket(USER_ONE, "other", "Close me");

        mockMvc.perform(put("/api/admin/support/tickets/{ticketId}/status", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"closed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.status").value("closed"));

        mockMvc.perform(post("/api/support/tickets/{ticketId}/messages", ticketId)
                        .header("Authorization", "Bearer " + USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"reopen?\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(post("/api/admin/support/tickets/{ticketId}/messages", ticketId)
                        .header("Authorization", "Bearer " + ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"public_reply\",\"content\":\"cannot reply\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void adminKeywordSearchCanMatchRelatedRecordId() throws Exception {
        mockMvc.perform(post("/api/support/tickets")
                        .header("Authorization", "Bearer " + USER_ONE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "order",
                                  "subject": "Need order help",
                                  "content": "Please review this order.",
                                  "relatedType": "order",
                                  "relatedId": 8001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.relatedId").value(8001));

        mockMvc.perform(get("/api/admin/support/tickets")
                        .header("Authorization", "Bearer " + SUPPORT_AGENT)
                        .param("keyword", "8001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].relatedId").value(8001));
    }

    private Number createTicket(String token, String category, String subject) throws Exception {
        String response = mockMvc.perform(post("/api/support/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "%s",
                                  "subject": "%s",
                                  "content": "Please help with this support case.",
                                  "priority": "normal"
                                }
                                """.formatted(category, subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ticket.status").value("open"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.ticket.id");
    }
}
