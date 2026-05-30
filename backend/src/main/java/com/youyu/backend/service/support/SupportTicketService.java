package com.youyu.backend.service.support;

import java.util.Map;

public interface SupportTicketService {

    Map<String, Object> createTicket(Long requesterUserId, Map<String, Object> command);

    Map<String, Object> listUserTickets(Long requesterUserId, String status, int page, int pageSize);

    Map<String, Object> userTicketDetail(Long requesterUserId, Long ticketId);

    Map<String, Object> addUserMessage(Long requesterUserId, Long ticketId, Map<String, Object> command);

    Map<String, Object> listAdminTickets(String status,
                                         String category,
                                         boolean assignedToMe,
                                         String keyword,
                                         int page,
                                         int pageSize,
                                         Long adminUserId);

    Map<String, Object> adminTicketDetail(Long ticketId);

    Map<String, Object> updateStatus(Long ticketId, Map<String, Object> command, Long adminUserId);

    Map<String, Object> addAdminMessage(Long ticketId, Map<String, Object> command, Long adminUserId);
}
