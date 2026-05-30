package com.youyu.backend.service.chat.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import com.youyu.backend.mapper.chat.ChatMessageMapper;
import com.youyu.backend.mapper.chat.SupportConsoleMapper;
import com.youyu.backend.service.chat.SupportConsoleService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportConsoleServiceImpl implements SupportConsoleService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final String CONVERSATION_TYPE_SUPPORT = "support";
    private static final String SUPPORT_STATUS_AI = "ai";
    private static final String SUPPORT_STATUS_PENDING = "pending";
    private static final String SUPPORT_STATUS_HUMAN = "human";
    private static final String SUPPORT_STATUS_CLOSED = "closed";

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final SupportConsoleMapper supportConsoleMapper;

    public SupportConsoleServiceImpl(ChatConversationMapper conversationMapper,
                                     ChatMessageMapper messageMapper,
                                     SupportConsoleMapper supportConsoleMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.supportConsoleMapper = supportConsoleMapper;
    }

    @Override
    public Map<String, Object> listQueue(String filter, Long adminId, int page, int size) {
        requireAdmin(adminId);
        page = Math.max(0, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int offset = page * size;

        List<Map<String, Object>> content = supportConsoleMapper.findQueue(filter, adminId, offset, size);
        int total = supportConsoleMapper.countQueue(filter, adminId);
        int totalPages = (int) Math.ceil((double) total / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("number", page);
        result.put("size", size);
        result.put("counts", supportConsoleMapper.countByStatus(adminId));
        return result;
    }

    @Override
    public Map<String, Object> getConversation(Long conversationId, Long adminId) {
        requireAdmin(adminId);
        Map<String, Object> conversation = requireSupportConversation(conversationId);
        return toConversationView(conversation);
    }

    @Override
    public Map<String, Object> getMessages(Long conversationId, Long adminId, int page, int size) {
        requireAdmin(adminId);
        requireSupportConversation(conversationId);
        page = Math.max(0, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int offset = page * size;

        List<Map<String, Object>> messages = messageMapper.findByConversationId(conversationId, offset, size);
        int total = messageMapper.countByConversationId(conversationId);
        int totalPages = (int) Math.ceil((double) total / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", messages);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> claim(Long conversationId, Long adminId) {
        requireAdmin(adminId);
        Map<String, Object> conversation = requireSupportConversation(conversationId);
        String status = statusOf(conversation);
        Long assignedAdminId = toLong(conversation.get("assignedAdminId"));
        if (SUPPORT_STATUS_HUMAN.equals(status) && assignedAdminId != null && !assignedAdminId.equals(adminId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该会话已被其他客服接入");
        }
        conversationMapper.assignSupportAgent(conversationId, adminId);
        markReadInternal(conversationId, conversation);
        return toConversationView(requireSupportConversation(conversationId));
    }

    @Override
    @Transactional
    public Map<String, Object> reply(Long conversationId, Long adminId, String body) {
        requireAdmin(adminId);
        Map<String, Object> conversation = requireSupportConversation(conversationId);
        String status = statusOf(conversation);
        if (SUPPORT_STATUS_CLOSED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "会话已结束，无法继续回复");
        }
        Long assignedAdminId = toLong(conversation.get("assignedAdminId"));
        if (assignedAdminId != null && !assignedAdminId.equals(adminId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该会话由其他客服处理，请勿重复接入");
        }
        String normalized = body == null ? "" : body.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "回复内容不能为空");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "回复内容不能超过 " + MAX_MESSAGE_LENGTH + " 字");
        }

        if (!SUPPORT_STATUS_HUMAN.equals(status)) {
            conversationMapper.assignSupportAgent(conversationId, adminId);
        }

        Long requesterId = toLong(conversation.get("userAId"));
        Long csUserId = toLong(conversation.get("userBId"));
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("conversationId", conversationId);
        message.put("senderUserId", csUserId);
        message.put("body", normalized);
        message.put("messageType", "text");
        message.put("mediaUrl", null);
        message.put("productId", null);
        message.put("orderId", null);
        message.put("isRead", false);
        message.put("readAt", null);
        message.put("createdAt", now);
        Long messageId = messageMapper.insert(message);
        conversationMapper.incrementUnreadCount(conversationId, requesterId);
        conversationMapper.updateLastMessageAt(conversationId, now.toString());
        markReadInternal(conversationId, conversation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", messageId);
        result.put("conversationId", conversationId);
        result.put("senderUserId", csUserId);
        result.put("body", normalized);
        result.put("messageType", "text");
        result.put("createdAt", now);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> close(Long conversationId, Long adminId) {
        requireAdmin(adminId);
        Map<String, Object> conversation = requireSupportConversation(conversationId);
        Long assignedAdminId = toLong(conversation.get("assignedAdminId"));
        if (assignedAdminId != null && !assignedAdminId.equals(adminId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该会话由其他客服处理，无法结束");
        }
        conversationMapper.updateSupportStatus(conversationId, SUPPORT_STATUS_CLOSED);
        conversationMapper.clearSupportAssignment(conversationId);
        return toConversationView(requireSupportConversation(conversationId));
    }

    @Override
    @Transactional
    public void markRead(Long conversationId, Long adminId) {
        requireAdmin(adminId);
        Map<String, Object> conversation = requireSupportConversation(conversationId);
        markReadInternal(conversationId, conversation);
    }

    private void markReadInternal(Long conversationId, Map<String, Object> conversation) {
        Long csUserId = toLong(conversation.get("userBId"));
        messageMapper.markMessagesRead(conversationId, csUserId, LocalDateTime.now());
        conversationMapper.clearUnreadCount(conversationId, csUserId);
    }

    private Map<String, Object> requireSupportConversation(Long conversationId) {
        Map<String, Object> conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!CONVERSATION_TYPE_SUPPORT.equals(String.valueOf(conversation.get("type")))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不是客服会话");
        }
        return conversation;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toConversationView(Map<String, Object> conversation) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long conversationId = toLong(conversation.get("id"));
        result.put("id", conversationId);
        result.put("type", conversation.get("type"));
        result.put("supportStatus", conversation.get("supportStatus"));
        result.put("assignedAdminId", conversation.get("assignedAdminId"));
        result.put("unreadCount", conversation.get("unreadCountB"));
        result.put("lastMessageAt", conversation.get("lastMessageAt"));
        result.put("createdAt", conversation.get("createdAt"));

        Map<String, Object> userA = (Map<String, Object>) conversation.get("userA");
        Map<String, Object> requester = new LinkedHashMap<>();
        if (userA != null) {
            requester.put("id", userA.get("id"));
            requester.put("username", userA.get("username"));
            requester.put("nickname", userA.get("nickname"));
            requester.put("avatar", userA.get("avatar"));
        }
        result.put("requester", requester);
        return result;
    }

    private String statusOf(Map<String, Object> conversation) {
        return String.valueOf(conversation.getOrDefault("supportStatus", ""));
    }

    private void requireAdmin(Long adminId) {
        if (adminId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.parseLong(value.toString());
    }
}
