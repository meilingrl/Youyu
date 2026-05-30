package com.youyu.backend.mapper.support;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SupportTicketMapper {

    Optional<Map<String, Object>> findTicketById(Long ticketId);

    Long insertTicket(Map<String, Object> command);

    Long insertMessage(Long ticketId, Long senderUserId, String senderRole, String messageType, String content);

    List<Map<String, Object>> findUserTickets(Long requesterUserId, String status, int offset, int limit);

    long countUserTickets(Long requesterUserId, String status);

    List<Map<String, Object>> findAdminTickets(String status,
                                               String category,
                                               Long assignedAdminUserId,
                                               String keyword,
                                               int offset,
                                               int limit);

    long countAdminTickets(String status, String category, Long assignedAdminUserId, String keyword);

    List<Map<String, Object>> findMessages(Long ticketId, boolean includeInternalNotes);

    int updateTicketAfterMessage(Long ticketId, String lastRepliedBy);

    int updateStatus(Long ticketId, String status, Long assignedAdminUserId, boolean assignAdmin);
}
