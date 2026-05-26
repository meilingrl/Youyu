package com.youyu.backend.service.chat.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import com.youyu.backend.mapper.chat.ChatMessageMapper;
import com.youyu.backend.service.chat.ChatService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_MEDIA_URL_LENGTH = 512;
    private static final int MAX_DATA_IMAGE_URL_LENGTH = 7 * 1024 * 1024;
    private static final int MAX_PAGE_SIZE_CONVERSATIONS = 50;
    private static final int MAX_PAGE_SIZE_MESSAGES = 100;
    private static final String MESSAGE_TYPE_TEXT = "text";
    private static final String MESSAGE_TYPE_IMAGE = "image";

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;

    public ChatServiceImpl(ChatConversationMapper conversationMapper, ChatMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public Map<String, Object> getConversations(Long userId, int page, int size) {
        requireLogin(userId);

        page = Math.max(0, page);
        size = Math.min(MAX_PAGE_SIZE_CONVERSATIONS, Math.max(1, size));
        int offset = page * size;

        List<Map<String, Object>> conversations = conversationMapper.findByUserId(userId, offset, size);
        int total = conversationMapper.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) total / size);

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
        requireLogin(currentUserId);
        if (peerUserId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "peerUserId is required");
        }
        if (currentUserId.equals(peerUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Cannot create a conversation with yourself");
        }

        Map<String, Object> existing = conversationMapper.findByParticipants(currentUserId, peerUserId, productId, shopId);
        if (existing != null) {
            return buildConversationResponse(existing, currentUserId);
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> conversationData = new LinkedHashMap<>();
        conversationData.put("type", determineConversationType(productId, shopId));
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
            existing = conversationMapper.findByParticipants(currentUserId, peerUserId, productId, shopId);
            if (existing != null) {
                return buildConversationResponse(existing, currentUserId);
            }
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Failed to create conversation");
        }

        Map<String, Object> created = conversationMapper.findById(conversationId);
        if (created == null) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Failed to create conversation");
        }
        return buildConversationResponse(created, currentUserId);
    }

    @Override
    public Map<String, Object> getMessages(Long conversationId, Long currentUserId, int page, int size) {
        requireLogin(currentUserId);
        requireParticipant(conversationId, currentUserId);

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
    public Map<String, Object> sendMessage(Long conversationId, Long currentUserId, String body, String messageType, String mediaUrl) {
        requireLogin(currentUserId);

        String normalizedType = normalizeMessageType(messageType);
        String normalizedBody = normalizeBody(body);
        String normalizedMediaUrl = normalizeMediaUrl(mediaUrl);
        validateMessagePayload(normalizedBody, normalizedType, normalizedMediaUrl);

        Map<String, Object> conversation = requireParticipant(conversationId, currentUserId);
        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> messageData = new LinkedHashMap<>();
        messageData.put("conversationId", conversationId);
        messageData.put("senderUserId", currentUserId);
        messageData.put("body", normalizedBody);
        messageData.put("messageType", normalizedType);
        messageData.put("mediaUrl", normalizedMediaUrl);
        messageData.put("isRead", false);
        messageData.put("readAt", null);
        messageData.put("createdAt", now);

        Long messageId = messageMapper.insert(messageData);
        conversationMapper.updateLastMessageAt(conversationId, now.toString());

        Long recipientUserId = currentUserId.equals(userAId) ? userBId : userAId;
        conversationMapper.incrementUnreadCount(conversationId, recipientUserId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", messageId);
        result.put("conversationId", conversationId);
        result.put("senderUserId", currentUserId);
        result.put("body", normalizedBody);
        result.put("messageType", normalizedType);
        result.put("mediaUrl", normalizedMediaUrl);
        result.put("isRead", false);
        result.put("readAt", null);
        result.put("createdAt", now);
        return result;
    }

    @Override
    public Map<String, Object> getUnreadCount(Long currentUserId) {
        requireLogin(currentUserId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", conversationMapper.sumUnreadCountByUserId(currentUserId));
        return result;
    }

    @Override
    @Transactional
    public void markConversationRead(Long conversationId, Long currentUserId) {
        requireLogin(currentUserId);
        requireParticipant(conversationId, currentUserId);

        messageMapper.markMessagesRead(conversationId, currentUserId, LocalDateTime.now());
        conversationMapper.clearUnreadCount(conversationId, currentUserId);
    }

    private Map<String, Object> requireParticipant(Long conversationId, Long currentUserId) {
        Map<String, Object> conversation = conversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Conversation not found");
        }

        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");
        if (!currentUserId.equals(userAId) && !currentUserId.equals(userBId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "No permission to access this conversation");
        }
        return conversation;
    }

    private void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Please log in first");
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildConversationResponse(Map<String, Object> conversation, Long currentUserId) {
        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");

        Map<String, Object> peerUser = currentUserId.equals(userAId)
                ? (Map<String, Object>) conversation.get("userB")
                : (Map<String, Object>) conversation.get("userA");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", conversation.get("id"));
        result.put("type", conversation.get("type"));
        result.put("productId", conversation.get("productId"));
        result.put("shopId", conversation.get("shopId"));
        result.put("peerUser", peerUser);
        result.put("unreadCount", currentUserId.equals(userAId)
                ? toInt(conversation.get("unreadCountA"))
                : toInt(conversation.get("unreadCountB")));
        result.put("lastMessageAt", conversation.get("lastMessageAt"));
        result.put("createdAt", conversation.get("createdAt"));
        return result;
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null || messageType.trim().isEmpty()) {
            return MESSAGE_TYPE_TEXT;
        }
        return messageType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBody(String body) {
        return body == null ? "" : body.trim();
    }

    private String normalizeMediaUrl(String mediaUrl) {
        return mediaUrl == null ? null : mediaUrl.trim();
    }

    private void validateMessagePayload(String body, String messageType, String mediaUrl) {
        if (!MESSAGE_TYPE_TEXT.equals(messageType) && !MESSAGE_TYPE_IMAGE.equals(messageType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported message type");
        }
        if (body.length() > MAX_MESSAGE_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Message body cannot exceed " + MAX_MESSAGE_LENGTH + " characters");
        }
        if (MESSAGE_TYPE_TEXT.equals(messageType) && body.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Message body cannot be empty");
        }
        if (MESSAGE_TYPE_TEXT.equals(messageType) && mediaUrl != null && !mediaUrl.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Text messages cannot include mediaUrl");
        }
        if (MESSAGE_TYPE_IMAGE.equals(messageType)) {
            if (mediaUrl == null || mediaUrl.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Image messages require mediaUrl");
            }
            boolean isDataImage = mediaUrl.startsWith("data:image/");
            int maxMediaUrlLength = isDataImage ? MAX_DATA_IMAGE_URL_LENGTH : MAX_MEDIA_URL_LENGTH;
            if (mediaUrl.length() > maxMediaUrlLength) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "mediaUrl cannot exceed " + maxMediaUrlLength + " characters");
            }
            if (!mediaUrl.startsWith("http://") && !mediaUrl.startsWith("https://") && !isDataImage) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "mediaUrl must start with http://, https://, or data:image/");
            }
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }
}
