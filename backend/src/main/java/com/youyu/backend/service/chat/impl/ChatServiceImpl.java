package com.youyu.backend.service.chat.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import com.youyu.backend.mapper.chat.ChatMessageMapper;
import com.youyu.backend.service.chat.ChatService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_PAGE_SIZE_CONVERSATIONS = 50;
    private static final int MAX_PAGE_SIZE_MESSAGES = 100;

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;

    public ChatServiceImpl(ChatConversationMapper conversationMapper, ChatMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public Map<String, Object> getConversations(Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        page = Math.max(0, page);
        size = Math.min(MAX_PAGE_SIZE_CONVERSATIONS, Math.max(1, size));
        int offset = page * size;

        List<Map<String, Object>> conversations = conversationMapper.findByUserId(userId, offset, size);
        int total = conversationMapper.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) total / size);

        // Transform conversations to include peerUser
        List<Map<String, Object>> content = conversations.stream()
                .map(conv -> buildConversationResponse(conv, userId))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("number", page);
        result.put("size", size);

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> findOrCreateConversation(Long currentUserId, Long peerUserId, Long productId, Long shopId) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (peerUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "对方用户ID不能为空");
        }
        if (currentUserId.equals(peerUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能与自己创建会话");
        }

        // Try to find existing conversation
        Map<String, Object> existing = conversationMapper.findByParticipants(currentUserId, peerUserId, productId, shopId);
        if (existing != null) {
            return buildConversationResponse(existing, currentUserId);
        }

        // Create new conversation
        String type = determineConversationType(productId, shopId);
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> conversationData = new LinkedHashMap<>();
        conversationData.put("type", type);
        conversationData.put("productId", productId);
        conversationData.put("shopId", shopId);
        conversationData.put("userAId", currentUserId);
        conversationData.put("userBId", peerUserId);
        conversationData.put("lastMessageAt", now);
        conversationData.put("createdAt", now);

        Long conversationId;
        try {
            conversationId = conversationMapper.insert(conversationData);
        } catch (DuplicateKeyException e) {
            // Race condition: another thread created the conversation
            existing = conversationMapper.findByParticipants(currentUserId, peerUserId, productId, shopId);
            if (existing != null) {
                return buildConversationResponse(existing, currentUserId);
            }
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "创建会话失败");
        }

        Map<String, Object> created = conversationMapper.findById(conversationId);
        if (created == null) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "创建会话失败");
        }

        return buildConversationResponse(created, currentUserId);
    }

    @Override
    public Map<String, Object> getMessages(Long conversationId, Long currentUserId, int page, int size) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        // Verify user is participant
        Map<String, Object> conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }

        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");
        if (!currentUserId.equals(userAId) && !currentUserId.equals(userBId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问此会话");
        }

        page = Math.max(0, page);
        size = Math.min(MAX_PAGE_SIZE_MESSAGES, Math.max(1, size));
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
    public Map<String, Object> sendMessage(Long conversationId, Long currentUserId, String body) {
        if (currentUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (body.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "消息内容不能超过" + MAX_MESSAGE_LENGTH + "字符");
        }

        // Verify user is participant
        Map<String, Object> conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }

        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");
        if (!currentUserId.equals(userAId) && !currentUserId.equals(userBId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权在此会话中发送消息");
        }

        // Insert message
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> messageData = new LinkedHashMap<>();
        messageData.put("conversationId", conversationId);
        messageData.put("senderUserId", currentUserId);
        messageData.put("body", body.trim());
        messageData.put("createdAt", now);

        Long messageId = messageMapper.insert(messageData);

        // Update conversation last_message_at
        conversationMapper.updateLastMessageAt(conversationId, now.toString());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", messageId);
        result.put("conversationId", conversationId);
        result.put("senderUserId", currentUserId);
        result.put("body", body.trim());
        result.put("createdAt", now);

        return result;
    }

    private String determineConversationType(Long productId, Long shopId) {
        if (productId != null) {
            return "product_inquiry";
        } else if (shopId != null) {
            return "shop_inquiry";
        } else {
            return "direct";
        }
    }

    private Map<String, Object> buildConversationResponse(Map<String, Object> conversation, Long currentUserId) {
        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");

        // Determine peer user (the other participant)
        Map<String, Object> peerUser;
        if (currentUserId.equals(userAId)) {
            peerUser = (Map<String, Object>) conversation.get("userB");
        } else {
            peerUser = (Map<String, Object>) conversation.get("userA");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", conversation.get("id"));
        result.put("type", conversation.get("type"));
        result.put("productId", conversation.get("productId"));
        result.put("shopId", conversation.get("shopId"));
        result.put("peerUser", peerUser);
        result.put("lastMessageAt", conversation.get("lastMessageAt"));
        result.put("createdAt", conversation.get("createdAt"));

        return result;
    }
}
