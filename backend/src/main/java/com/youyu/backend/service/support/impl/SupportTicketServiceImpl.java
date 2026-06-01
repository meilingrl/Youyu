package com.youyu.backend.service.support.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.common.exception.ForbiddenException;
import com.youyu.backend.mapper.support.SupportTicketMapper;
import com.youyu.backend.service.notification.NotificationService;
import com.youyu.backend.service.support.SupportTicketService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketServiceImpl implements SupportTicketService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter TICKET_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> STATUSES = List.of("open", "in_progress", "waiting_user", "resolved", "closed");
    private static final List<String> CATEGORIES = List.of("account", "order", "product", "shop", "payment", "report", "other");
    private static final List<String> MESSAGE_TYPES = List.of("public_reply", "internal_note");

    private final SupportTicketMapper supportTicketMapper;
    private final NotificationService notificationService;

    public SupportTicketServiceImpl(SupportTicketMapper supportTicketMapper,
                                    NotificationService notificationService) {
        this.supportTicketMapper = supportTicketMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Map<String, Object> createTicket(Long requesterUserId, Map<String, Object> command) {
        String category = requireAllowed(asString(command.get("category")), CATEGORIES, "Unsupported support category");
        String subject = requireText(asString(command.get("subject")), "subject is required", 120);
        String content = requireText(asString(command.get("content")), "content is required", 5000);
        String priority = optionalText(asString(command.get("priority")), "normal", 32);
        String relatedType = optionalText(asString(command.get("relatedType")), null, 32);
        Long relatedId = nullableLong(command.get("relatedId"));

        Long ticketId = null;
        for (int attempt = 0; attempt < 3 && ticketId == null; attempt++) {
            try {
                ticketId = supportTicketMapper.insertTicket(linkedMap(
                        "ticketNo", nextTicketNo(),
                        "requesterUserId", requesterUserId,
                        "category", category,
                        "subject", subject,
                        "content", content,
                        "priority", priority,
                        "relatedType", relatedType,
                        "relatedId", relatedId
                ));
            } catch (DuplicateKeyException exception) {
                if (attempt == 2) {
                    throw exception;
                }
            }
        }
        supportTicketMapper.insertMessage(ticketId, requesterUserId, "user", "public_reply", content);
        return linkedMap("ticket", findTicket(ticketId), "messages", supportTicketMapper.findMessages(ticketId, false));
    }

    @Override
    public Map<String, Object> listUserTickets(Long requesterUserId, String status, int page, int pageSize) {
        String normalizedStatus = optionalAllowed(status, STATUSES, "Unsupported support ticket status");
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        return linkedMap(
                "items", supportTicketMapper.findUserTickets(requesterUserId, normalizedStatus, offset, ps),
                "total", supportTicketMapper.countUserTickets(requesterUserId, normalizedStatus),
                "page", pg,
                "pageSize", ps
        );
    }

    @Override
    public Map<String, Object> userTicketDetail(Long requesterUserId, Long ticketId) {
        Map<String, Object> ticket = findTicket(ticketId);
        requireRequester(ticket, requesterUserId);
        return linkedMap("ticket", ticket, "messages", supportTicketMapper.findMessages(ticketId, false));
    }

    @Override
    @Transactional
    public Map<String, Object> addUserMessage(Long requesterUserId, Long ticketId, Map<String, Object> command) {
        Map<String, Object> ticket = findTicket(ticketId);
        requireRequester(ticket, requesterUserId);
        rejectClosed(ticket);
        String content = requireText(asString(command.get("content")), "content is required", 5000);
        supportTicketMapper.insertMessage(ticketId, requesterUserId, "user", "public_reply", content);
        supportTicketMapper.updateTicketAfterMessage(ticketId, "user");
        return linkedMap("ticket", findTicket(ticketId), "messages", supportTicketMapper.findMessages(ticketId, false));
    }

    @Override
    public Map<String, Object> listAdminTickets(String status,
                                                String category,
                                                boolean assignedToMe,
                                                String keyword,
                                                int page,
                                                int pageSize,
                                                Long adminUserId) {
        String normalizedStatus = optionalAllowed(status, STATUSES, "Unsupported support ticket status");
        String normalizedCategory = optionalAllowed(category, CATEGORIES, "Unsupported support category");
        Long assignee = assignedToMe ? adminUserId : null;
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        return linkedMap(
                "items", supportTicketMapper.findAdminTickets(normalizedStatus, normalizedCategory, assignee, keyword, offset, ps),
                "total", supportTicketMapper.countAdminTickets(normalizedStatus, normalizedCategory, assignee, keyword),
                "page", pg,
                "pageSize", ps
        );
    }

    @Override
    public Map<String, Object> adminTicketDetail(Long ticketId) {
        return linkedMap("ticket", findTicket(ticketId), "messages", supportTicketMapper.findMessages(ticketId, true));
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Long ticketId, Map<String, Object> command, Long adminUserId) {
        Map<String, Object> ticket = findTicket(ticketId);
        String currentStatus = String.valueOf(ticket.get("status"));
        String nextStatus = requireAllowed(asString(command.get("status")), STATUSES, "Unsupported support ticket status");
        if ("closed".equals(currentStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Closed support tickets cannot change status");
        }
        if (!currentStatus.equals(nextStatus) && !allowedNextStatuses(currentStatus).contains(nextStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Invalid support ticket status transition");
        }
        boolean assignToMe = toBoolean(command.get("assignToMe"));
        supportTicketMapper.updateStatus(ticketId, nextStatus, adminUserId, assignToMe);
        Map<String, Object> updatedTicket = findTicket(ticketId);
        if (!currentStatus.equals(nextStatus)) {
            notificationService.createSupportTicketNotification(
                    toLong(updatedTicket.get("requesterUserId")),
                    updatedTicket,
                    "Support ticket status updated",
                    "Ticket " + ticketLabel(updatedTicket) + " moved to " + nextStatus + "."
            );
        }
        return linkedMap("ticket", updatedTicket);
    }

    @Override
    @Transactional
    public Map<String, Object> addAdminMessage(Long ticketId, Map<String, Object> command, Long adminUserId) {
        Map<String, Object> ticket = findTicket(ticketId);
        rejectClosed(ticket);
        String messageType = requireAllowed(asString(command.get("messageType")), MESSAGE_TYPES, "Unsupported support message type");
        String content = requireText(asString(command.get("content")), "content is required", 5000);
        supportTicketMapper.insertMessage(ticketId, adminUserId, "admin", messageType, content);
        supportTicketMapper.updateTicketAfterMessage(ticketId, "admin");
        Map<String, Object> updatedTicket = findTicket(ticketId);
        if ("public_reply".equals(messageType)) {
            notificationService.createSupportTicketNotification(
                    toLong(updatedTicket.get("requesterUserId")),
                    updatedTicket,
                    "Platform support replied",
                    "Ticket " + ticketLabel(updatedTicket) + " has a new public reply from platform support."
            );
        }
        return linkedMap("ticket", updatedTicket, "messages", supportTicketMapper.findMessages(ticketId, true));
    }

    private Map<String, Object> findTicket(Long ticketId) {
        return supportTicketMapper.findTicketById(ticketId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Support ticket does not exist"));
    }

    private void requireRequester(Map<String, Object> ticket, Long requesterUserId) {
        if (!toLong(ticket.get("requesterUserId")).equals(requesterUserId)) {
            throw new ForbiddenException("Cannot access another user's support ticket");
        }
    }

    private void rejectClosed(Map<String, Object> ticket) {
        if ("closed".equals(ticket.get("status"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Closed support tickets cannot receive replies");
        }
    }

    private List<String> allowedNextStatuses(String currentStatus) {
        return switch (currentStatus) {
            case "open" -> List.of("in_progress", "closed");
            case "in_progress" -> List.of("waiting_user", "resolved", "closed");
            case "waiting_user" -> List.of("in_progress", "resolved", "closed");
            case "resolved" -> List.of("closed");
            default -> List.of();
        };
    }

    private int clampPageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String nextTicketNo() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "SUP-" + LocalDateTime.now().format(TICKET_NO_FORMATTER) + "-" + suffix;
    }

    private String requireText(String value, String message, int maxLength) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message + " length exceeds " + maxLength);
        }
        return normalized;
    }

    private String optionalText(String value, String defaultValue, int maxLength) {
        if (value == null || value.trim().isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Field length exceeds " + maxLength);
        }
        return normalized;
    }

    private String requireAllowed(String value, List<String> allowedValues, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || !allowedValues.contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String optionalAllowed(String value, List<String> allowedValues, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Long nullableLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return toLong(value);
    }

    private String ticketLabel(Map<String, Object> ticket) {
        Object ticketNo = ticket.get("ticketNo");
        if (ticketNo != null && !String.valueOf(ticketNo).isBlank()) {
            return String.valueOf(ticketNo);
        }
        return "#" + ticket.get("id");
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
