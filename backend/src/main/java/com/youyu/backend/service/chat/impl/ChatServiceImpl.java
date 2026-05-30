package com.youyu.backend.service.chat.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.chat.AutoReplySettingsMapper;
import com.youyu.backend.mapper.chat.ChatConversationMapper;
import com.youyu.backend.mapper.chat.ChatMessageMapper;
import com.youyu.backend.service.chat.ChatService;
import com.youyu.backend.service.chat.SupportFaqKnowledgeBase;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private static final int AUTO_REPLY_MAX_LENGTH = 500;
    private static final Duration RECALL_WINDOW = Duration.ofMinutes(2);
    private static final Duration AUTO_REPLY_WINDOW = Duration.ofHours(24);
    private static final String DEFAULT_AUTO_REPLY = "您好，我现在不方便及时回复，稍后看到消息会尽快联系您。";
    private static final String MESSAGE_TYPE_TEXT = "text";
    private static final String MESSAGE_TYPE_IMAGE = "image";
    private static final String MESSAGE_TYPE_PRODUCT_CARD = "product_card";
    private static final String MESSAGE_TYPE_ORDER_CARD = "order_card";
    private static final String CONVERSATION_TYPE_SUPPORT = "support";
    private static final String SUPPORT_STATUS_AI = "ai";
    private static final String SUPPORT_STATUS_PENDING = "pending";
    private static final String SUPPORT_STATUS_HUMAN = "human";
    private static final String SUPPORT_STATUS_CLOSED = "closed";

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final AutoReplySettingsMapper autoReplySettingsMapper;
    private final SupportFaqKnowledgeBase supportFaqKnowledgeBase;

    public ChatServiceImpl(ChatConversationMapper conversationMapper,
                           ChatMessageMapper messageMapper,
                           AutoReplySettingsMapper autoReplySettingsMapper,
                           SupportFaqKnowledgeBase supportFaqKnowledgeBase) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.autoReplySettingsMapper = autoReplySettingsMapper;
        this.supportFaqKnowledgeBase = supportFaqKnowledgeBase;
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
    public Map<String, Object> searchMessages(Long currentUserId, String keyword, String startTime, String endTime, int page, int size) {
        requireLogin(currentUserId);

        page = Math.max(0, page);
        size = Math.min(MAX_PAGE_SIZE_MESSAGES, Math.max(1, size));
        int offset = page * size;
        LocalDateTime start = parseDateTime(startTime, "startTime");
        LocalDateTime end = parseDateTime(endTime, "endTime");
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "startTime cannot be after endTime");
        }

        List<Map<String, Object>> messages = messageMapper.searchByUser(currentUserId, normalizeNullableKeyword(keyword), start, end, offset, size);
        int total = messageMapper.countSearchByUser(currentUserId, normalizeNullableKeyword(keyword), start, end);
        int totalPages = (int) Math.ceil((double) total / size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", messages);
        result.put("total", total);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);
        result.put("page", page);
        result.put("number", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> sendMessage(Long conversationId, Long currentUserId, String body, String messageType, String mediaUrl, Long productId, Long orderId) {
        requireLogin(currentUserId);

        String normalizedType = normalizeMessageType(messageType);
        String normalizedBody = normalizeBody(body);
        String normalizedMediaUrl = normalizeMediaUrl(mediaUrl);
        validateMessagePayload(normalizedBody, normalizedType, normalizedMediaUrl, productId, orderId);

        Map<String, Object> conversation = requireParticipant(conversationId, currentUserId);
        assertSupportSessionOpen(conversation);
        Long userAId = (Long) conversation.get("userAId");
        Long userBId = (Long) conversation.get("userBId");
        Map<String, Object> product = validateProductCard(normalizedType, productId);
        Map<String, Object> order = validateOrderCard(normalizedType, orderId, currentUserId);

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> messageData = new LinkedHashMap<>();
        messageData.put("conversationId", conversationId);
        messageData.put("senderUserId", currentUserId);
        messageData.put("body", normalizedBody);
        messageData.put("messageType", normalizedType);
        messageData.put("mediaUrl", normalizedMediaUrl);
        messageData.put("productId", MESSAGE_TYPE_PRODUCT_CARD.equals(normalizedType) ? productId : null);
        messageData.put("orderId", MESSAGE_TYPE_ORDER_CARD.equals(normalizedType) ? orderId : null);
        messageData.put("isRead", false);
        messageData.put("readAt", null);
        messageData.put("createdAt", now);

        Long messageId = messageMapper.insert(messageData);
        conversationMapper.updateLastMessageAt(conversationId, now.toString());

        Long recipientUserId = currentUserId.equals(userAId) ? userBId : userAId;
        conversationMapper.incrementUnreadCount(conversationId, recipientUserId);
        if (CONVERSATION_TYPE_SUPPORT.equals(String.valueOf(conversation.get("type")))) {
            maybeSupportAiReply(conversation, currentUserId, normalizedType, normalizedBody, now);
        } else {
            maybeSendAutoReply(conversation, currentUserId, recipientUserId, now);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", messageId);
        result.put("conversationId", conversationId);
        result.put("senderUserId", currentUserId);
        result.put("body", normalizedBody);
        result.put("messageType", normalizedType);
        result.put("mediaUrl", normalizedMediaUrl);
        result.put("productId", MESSAGE_TYPE_PRODUCT_CARD.equals(normalizedType) ? productId : null);
        result.put("orderId", MESSAGE_TYPE_ORDER_CARD.equals(normalizedType) ? orderId : null);
        result.put("product", product);
        result.put("order", sanitizeOrderCard(order));
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

    @Override
    @Transactional
    public void updatePinStatus(Long conversationId, Long currentUserId, boolean pinned) {
        requireLogin(currentUserId);
        requireParticipant(conversationId, currentUserId);
        conversationMapper.updatePinStatus(conversationId, currentUserId, pinned);
    }

    @Override
    @Transactional
    public void updateMuteStatus(Long conversationId, Long currentUserId, boolean muted) {
        requireLogin(currentUserId);
        requireParticipant(conversationId, currentUserId);
        conversationMapper.updateMuteStatus(conversationId, currentUserId, muted);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Long currentUserId) {
        requireLogin(currentUserId);
        Map<String, Object> conversation = requireParticipant(conversationId, currentUserId);
        if (CONVERSATION_TYPE_SUPPORT.equals(String.valueOf(conversation.get("type")))) {
            closeSupportSessionInternal(conversation, currentUserId, false);
        }
        conversationMapper.softDelete(conversationId, currentUserId);
        conversationMapper.clearUnreadCount(conversationId, currentUserId);
    }

    @Override
    @Transactional
    public void recallMessage(Long messageId, Long currentUserId) {
        requireLogin(currentUserId);
        Map<String, Object> message = messageMapper.findById(messageId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Message not found"));
        requireParticipant(toLong(message.get("conversationId")), currentUserId);
        if (!Objects.equals(toLong(message.get("senderUserId")), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the sender can recall this message");
        }
        if (toBoolean(message.get("isRecalled"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Message has already been recalled");
        }
        LocalDateTime createdAt = toLocalDateTime(message.get("createdAt"));
        if (createdAt == null || createdAt.isBefore(LocalDateTime.now().minus(RECALL_WINDOW))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Only messages sent within 2 minutes can be recalled");
        }
        messageMapper.recall(messageId, LocalDateTime.now());
    }

    @Override
    public Map<String, Object> getAutoReplySettings(Long currentUserId) {
        requireLogin(currentUserId);
        return autoReplySettingsMapper.findByUserId(currentUserId)
                .map(this::normalizeAutoReplySettings)
                .orElseGet(() -> {
                    Map<String, Object> defaults = new LinkedHashMap<>();
                    defaults.put("isEnabled", false);
                    defaults.put("replyContent", DEFAULT_AUTO_REPLY);
                    return defaults;
                });
    }

    @Override
    @Transactional
    public void updateAutoReplySettings(Long currentUserId, Boolean enabled, String replyContent) {
        requireLogin(currentUserId);
        autoReplySettingsMapper.upsert(currentUserId, Boolean.TRUE.equals(enabled), normalizeAutoReplyContent(replyContent));
    }

    @Override
    @Transactional
    public Map<String, Object> startSupportSession(Long currentUserId) {
        requireLogin(currentUserId);
        Long csUserId = conversationMapper.findPlatformCsUserId();
        if (csUserId == null) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "平台客服账号未配置，请联系管理员或重启后端服务");
        }
        if (csUserId.equals(currentUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Customer-service account cannot start a support session");
        }

        Map<String, Object> existing = conversationMapper.findSupportByRequesterAndCs(currentUserId, csUserId);
        if (existing == null) {
            Map<String, Object> legacy = conversationMapper.findByParticipants(currentUserId, csUserId, null, null);
            if (legacy != null) {
                Long legacyId = toLong(legacy.get("id"));
                conversationMapper.updateType(legacyId, CONVERSATION_TYPE_SUPPORT);
                requireSupportStatusUpdate(legacyId, SUPPORT_STATUS_AI);
                existing = conversationMapper.findById(legacyId);
            }
        }
        if (existing != null) {
            Long conversationId = toLong(existing.get("id"));
            String status = String.valueOf(existing.getOrDefault("supportStatus", ""));
            if (SUPPORT_STATUS_CLOSED.equals(status)) {
                reopenSupportSession(conversationId);
            } else if (status.isBlank() || "null".equals(status)) {
                requireSupportStatusUpdate(conversationId, SUPPORT_STATUS_AI);
            }
            conversationMapper.restoreForUser(conversationId, currentUserId);
            Map<String, Object> reopened = conversationMapper.findById(conversationId);
            if (SUPPORT_STATUS_CLOSED.equals(String.valueOf(reopened.getOrDefault("supportStatus", "")))) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "无法重新开启客服会话，请稍后重试");
            }
            return buildConversationResponse(reopened, currentUserId);
        }

        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> conversationData = new LinkedHashMap<>();
        conversationData.put("type", CONVERSATION_TYPE_SUPPORT);
        conversationData.put("productId", null);
        conversationData.put("shopId", null);
        conversationData.put("userAId", currentUserId);
        conversationData.put("userBId", csUserId);
        conversationData.put("lastMessageAt", now);
        conversationData.put("createdAt", now);

        Long conversationId;
        try {
            conversationId = conversationMapper.insert(conversationData);
        } catch (DuplicateKeyException e) {
            existing = conversationMapper.findByParticipants(currentUserId, csUserId, null, null);
            if (existing != null) {
                return buildConversationResponse(existing, currentUserId);
            }
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Failed to start support session");
        }
        requireSupportStatusUpdate(conversationId, SUPPORT_STATUS_AI);

        Map<String, Object> greeting = new LinkedHashMap<>();
        greeting.put("conversationId", conversationId);
        greeting.put("senderUserId", csUserId);
        greeting.put("body", SupportFaqKnowledgeBase.GREETING);
        greeting.put("messageType", MESSAGE_TYPE_TEXT);
        greeting.put("mediaUrl", null);
        greeting.put("productId", null);
        greeting.put("orderId", null);
        greeting.put("isRead", false);
        greeting.put("readAt", null);
        greeting.put("createdAt", now.plusNanos(1000));
        messageMapper.insert(greeting);
        conversationMapper.incrementUnreadCount(conversationId, currentUserId);
        conversationMapper.updateLastMessageAt(conversationId, now.plusNanos(1000).toString());

        Map<String, Object> created = conversationMapper.findById(conversationId);
        if (created == null) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Failed to start support session");
        }
        return buildConversationResponse(created, currentUserId);
    }

    @Override
    @Transactional
    public Map<String, Object> closeSupportSession(Long conversationId, Long currentUserId) {
        requireLogin(currentUserId);
        Map<String, Object> conversation = requireParticipant(conversationId, currentUserId);
        if (!CONVERSATION_TYPE_SUPPORT.equals(String.valueOf(conversation.get("type")))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Not a support conversation");
        }
        Long requesterId = toLong(conversation.get("userAId"));
        if (!currentUserId.equals(requesterId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the requester can end a support session");
        }
        closeSupportSessionInternal(conversation, currentUserId, true);
        return buildConversationResponse(conversationMapper.findById(conversationId), currentUserId);
    }

    @Override
    @Transactional
    public void escalateSupportConversation(Long conversationId, Long currentUserId) {
        requireLogin(currentUserId);
        Map<String, Object> conversation = requireParticipant(conversationId, currentUserId);
        conversation = ensureSupportConversationForEscalation(conversation, conversationId);
        String status = String.valueOf(conversation.getOrDefault("supportStatus", ""));
        if (SUPPORT_STATUS_CLOSED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Support session is already closed");
        }
        if (!SUPPORT_STATUS_PENDING.equals(status) && !SUPPORT_STATUS_HUMAN.equals(status)) {
            requireSupportStatusUpdate(conversationId, SUPPORT_STATUS_PENDING);
            conversationMapper.clearSupportAssignment(conversationId);

            LocalDateTime replyAt = LocalDateTime.now();
            Long csUserId = toLong(conversation.get("userBId"));
            Map<String, Object> systemNote = new LinkedHashMap<>();
            systemNote.put("conversationId", conversationId);
            systemNote.put("senderUserId", csUserId);
            systemNote.put("body", "已为你转接人工客服，平台客服会尽快接入处理，请稍候。");
            systemNote.put("messageType", MESSAGE_TYPE_TEXT);
            systemNote.put("mediaUrl", null);
            systemNote.put("productId", null);
            systemNote.put("orderId", null);
            systemNote.put("isRead", false);
            systemNote.put("readAt", null);
            systemNote.put("createdAt", replyAt);
            messageMapper.insert(systemNote);
            conversationMapper.incrementUnreadCount(conversationId, currentUserId);
            if (csUserId != null) {
                conversationMapper.incrementUnreadCount(conversationId, csUserId);
            }
            conversationMapper.updateLastMessageAt(conversationId, replyAt.toString());
        }
    }

    private Map<String, Object> ensureSupportConversationForEscalation(Map<String, Object> conversation, Long conversationId) {
        if (CONVERSATION_TYPE_SUPPORT.equals(String.valueOf(conversation.get("type")))) {
            return conversation;
        }
        Long csUserId = conversationMapper.findPlatformCsUserId();
        Long userAId = toLong(conversation.get("userAId"));
        Long userBId = toLong(conversation.get("userBId"));
        if (csUserId == null || (!csUserId.equals(userAId) && !csUserId.equals(userBId))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Not a support conversation");
        }
        conversationMapper.updateType(conversationId, CONVERSATION_TYPE_SUPPORT);
        String status = String.valueOf(conversation.getOrDefault("supportStatus", ""));
        if (status.isBlank() || "null".equals(status) || SUPPORT_STATUS_CLOSED.equals(status)) {
            requireSupportStatusUpdate(conversationId, SUPPORT_STATUS_AI);
        }
        Map<String, Object> refreshed = conversationMapper.findById(conversationId);
        if (refreshed == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Conversation not found");
        }
        return refreshed;
    }

    private void reopenSupportSession(Long conversationId) {
        requireSupportStatusUpdate(conversationId, SUPPORT_STATUS_AI);
        conversationMapper.clearSupportAssignment(conversationId);
    }

    private void closeSupportSessionInternal(Map<String, Object> conversation, Long currentUserId, boolean notifyUser) {
        Long conversationId = toLong(conversation.get("id"));
        String status = String.valueOf(conversation.getOrDefault("supportStatus", ""));
        if (SUPPORT_STATUS_CLOSED.equals(status)) {
            return;
        }
        requireSupportStatusUpdate(conversationId, SUPPORT_STATUS_CLOSED);
        conversationMapper.clearSupportAssignment(conversationId);
        if (!notifyUser) {
            return;
        }
        Long csUserId = toLong(conversation.get("userBId"));
        LocalDateTime replyAt = LocalDateTime.now();
        Map<String, Object> systemNote = new LinkedHashMap<>();
        systemNote.put("conversationId", conversationId);
        systemNote.put("senderUserId", csUserId);
        systemNote.put("body", "本次客服会话已结束。如需继续咨询，请点击「再次咨询」重新发起。");
        systemNote.put("messageType", MESSAGE_TYPE_TEXT);
        systemNote.put("mediaUrl", null);
        systemNote.put("productId", null);
        systemNote.put("orderId", null);
        systemNote.put("isRead", false);
        systemNote.put("readAt", null);
        systemNote.put("createdAt", replyAt);
        messageMapper.insert(systemNote);
        conversationMapper.incrementUnreadCount(conversationId, currentUserId);
        conversationMapper.updateLastMessageAt(conversationId, replyAt.toString());
    }

    private void assertSupportSessionOpen(Map<String, Object> conversation) {
        if (!CONVERSATION_TYPE_SUPPORT.equals(String.valueOf(conversation.get("type")))) {
            return;
        }
        String status = String.valueOf(conversation.getOrDefault("supportStatus", ""));
        if (SUPPORT_STATUS_CLOSED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "客服会话已结束，请点击「再次咨询」重新发起");
        }
    }

    private void requireSupportStatusUpdate(Long conversationId, String expectedStatus) {
        int updated = conversationMapper.updateSupportStatus(conversationId, expectedStatus);
        if (updated > 0) {
            return;
        }
        Map<String, Object> fresh = conversationMapper.findById(conversationId);
        String actual = fresh == null ? "" : String.valueOf(fresh.getOrDefault("supportStatus", ""));
        if (expectedStatus.equals(actual)) {
            return;
        }
        throw new BusinessException(
                ResultCode.INTERNAL_SERVER_ERROR,
                "客服会话状态更新失败，请确认数据库已执行 support 字段迁移后重启后端"
        );
    }

    private void maybeSupportAiReply(Map<String, Object> conversation, Long senderUserId, String messageType, String body, LocalDateTime originalMessageAt) {
        Long requesterId = toLong(conversation.get("userAId"));
        Long csUserId = toLong(conversation.get("userBId"));
        if (!senderUserId.equals(requesterId)) {
            return;
        }
        String status = String.valueOf(conversation.getOrDefault("supportStatus", ""));
        if (!SUPPORT_STATUS_AI.equals(status)) {
            return;
        }

        String answer = MESSAGE_TYPE_TEXT.equals(messageType)
                ? supportFaqKnowledgeBase.answer(body)
                : SupportFaqKnowledgeBase.FALLBACK_REPLY;

        Long conversationId = toLong(conversation.get("id"));
        LocalDateTime replyAt = originalMessageAt.plusNanos(1000);
        Map<String, Object> autoMessage = new LinkedHashMap<>();
        autoMessage.put("conversationId", conversationId);
        autoMessage.put("senderUserId", csUserId);
        autoMessage.put("body", answer);
        autoMessage.put("messageType", MESSAGE_TYPE_TEXT);
        autoMessage.put("mediaUrl", null);
        autoMessage.put("productId", null);
        autoMessage.put("orderId", null);
        autoMessage.put("isRead", false);
        autoMessage.put("readAt", null);
        autoMessage.put("createdAt", replyAt);
        messageMapper.insert(autoMessage);
        conversationMapper.incrementUnreadCount(conversationId, requesterId);
        conversationMapper.updateLastMessageAt(conversationId, replyAt.toString());
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
        result.put("supportStatus", conversation.get("supportStatus"));
        result.put("assignedAdminId", conversation.get("assignedAdminId"));
        result.put("productId", conversation.get("productId"));
        result.put("shopId", conversation.get("shopId"));
        result.put("peerUser", peerUser);
        boolean isMuted = currentUserId.equals(userAId)
                ? toBoolean(conversation.get("isMutedA"))
                : toBoolean(conversation.get("isMutedB"));
        result.put("unreadCount", isMuted ? 0 : (currentUserId.equals(userAId)
                ? toInt(conversation.get("unreadCountA"))
                : toInt(conversation.get("unreadCountB"))));
        result.put("isPinned", currentUserId.equals(userAId)
                ? toBoolean(conversation.get("isPinnedA"))
                : toBoolean(conversation.get("isPinnedB")));
        result.put("isMuted", isMuted);
        result.put("deletedAt", currentUserId.equals(userAId)
                ? conversation.get("deletedByAAt")
                : conversation.get("deletedByBAt"));
        Map<String, Object> lastMessage = latestMessage((Long) conversation.get("id"));
        result.put("lastMessage", lastMessage);
        result.put("lastMessagePreview", previewFor(lastMessage));
        result.put("lastMessageType", lastMessage == null ? null : lastMessage.get("messageType"));
        result.put("lastMessageAt", conversation.get("lastMessageAt"));
        result.put("createdAt", conversation.get("createdAt"));
        return result;
    }

    private Map<String, Object> latestMessage(Long conversationId) {
        List<Map<String, Object>> messages = messageMapper.findByConversationId(conversationId, 0, 1);
        return messages.isEmpty() ? null : messages.get(0);
    }

    private String previewFor(Map<String, Object> message) {
        if (message == null) {
            return "开始对话";
        }
        if (toBoolean(message.get("isRecalled"))) {
            return "消息已撤回";
        }
        String type = String.valueOf(message.getOrDefault("messageType", MESSAGE_TYPE_TEXT));
        return switch (type) {
            case MESSAGE_TYPE_IMAGE -> "[图片]";
            case MESSAGE_TYPE_PRODUCT_CARD -> "[商品卡片]";
            case MESSAGE_TYPE_ORDER_CARD -> "[订单卡片]";
            default -> {
                String body = String.valueOf(message.getOrDefault("body", ""));
                yield body.isBlank() ? "开始对话" : body;
            }
        };
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

    private String normalizeNullableKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String normalized = value.trim();
            if (normalized.endsWith("Z")) {
                return java.time.OffsetDateTime.parse(normalized).toLocalDateTime();
            }
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " must be ISO-8601 datetime");
        }
    }

    private String normalizeMediaUrl(String mediaUrl) {
        return mediaUrl == null ? null : mediaUrl.trim();
    }

    private void validateMessagePayload(String body, String messageType, String mediaUrl, Long productId, Long orderId) {
        if (!MESSAGE_TYPE_TEXT.equals(messageType)
                && !MESSAGE_TYPE_IMAGE.equals(messageType)
                && !MESSAGE_TYPE_PRODUCT_CARD.equals(messageType)
                && !MESSAGE_TYPE_ORDER_CARD.equals(messageType)) {
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
        if ((MESSAGE_TYPE_TEXT.equals(messageType) || MESSAGE_TYPE_IMAGE.equals(messageType))
                && (productId != null || orderId != null)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Only card messages can include productId or orderId");
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
        if (MESSAGE_TYPE_PRODUCT_CARD.equals(messageType)) {
            if (productId == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Product card messages require productId");
            }
            if (orderId != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Product card messages cannot include orderId");
            }
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Product card messages cannot include mediaUrl");
            }
        }
        if (MESSAGE_TYPE_ORDER_CARD.equals(messageType)) {
            if (orderId == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Order card messages require orderId");
            }
            if (productId != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Order card messages cannot include productId");
            }
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Order card messages cannot include mediaUrl");
            }
        }
    }

    private Map<String, Object> validateProductCard(String messageType, Long productId) {
        if (!MESSAGE_TYPE_PRODUCT_CARD.equals(messageType)) {
            return null;
        }
        Map<String, Object> product = messageMapper.findProductCardSummary(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Product not found"));
        if (!"on_sale".equals(String.valueOf(product.get("status")))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Product is not shareable");
        }
        Object reviewStatus = product.get("reviewStatus");
        if (reviewStatus != null
                && !"approved".equals(String.valueOf(reviewStatus))
                && !"not_required".equals(String.valueOf(reviewStatus))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Product is not shareable");
        }
        return withoutReviewStatus(product);
    }

    private Map<String, Object> validateOrderCard(String messageType, Long orderId, Long currentUserId) {
        if (!MESSAGE_TYPE_ORDER_CARD.equals(messageType)) {
            return null;
        }
        Map<String, Object> order = messageMapper.findOrderCardSummary(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Order not found"));
        Long buyerUserId = toLong(order.get("buyerUserId"));
        Long sellerUserId = toLong(order.get("sellerUserId"));
        if (!Objects.equals(currentUserId, buyerUserId) && !Objects.equals(currentUserId, sellerUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "No permission to share this order");
        }
        return order;
    }

    private Map<String, Object> withoutReviewStatus(Map<String, Object> product) {
        if (product == null) {
            return null;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>(product);
        sanitized.remove("reviewStatus");
        return sanitized;
    }

    private Map<String, Object> sanitizeOrderCard(Map<String, Object> order) {
        if (order == null) {
            return null;
        }
        Map<String, Object> sanitized = new LinkedHashMap<>(order);
        sanitized.remove("buyerUserId");
        sanitized.remove("sellerUserId");
        return sanitized;
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

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(value.toString());
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

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value == null) {
            return null;
        }
        return LocalDateTime.parse(value.toString().replace(" ", "T"));
    }

    private Map<String, Object> normalizeAutoReplySettings(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isEnabled", toBoolean(raw.get("isEnabled")));
        result.put("replyContent", raw.getOrDefault("replyContent", DEFAULT_AUTO_REPLY));
        result.put("updatedAt", raw.get("updatedAt"));
        return result;
    }

    private String normalizeAutoReplyContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "replyContent is required");
        }
        if (text.length() > AUTO_REPLY_MAX_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "replyContent cannot exceed " + AUTO_REPLY_MAX_LENGTH + " characters");
        }
        return text;
    }

    private void maybeSendAutoReply(Map<String, Object> conversation, Long senderUserId, Long autoReplyUserId, LocalDateTime originalMessageAt) {
        Map<String, Object> settings = autoReplySettingsMapper.findByUserId(autoReplyUserId).orElse(null);
        if (settings == null || !toBoolean(settings.get("isEnabled"))) {
            return;
        }
        Long conversationId = toLong(conversation.get("id"));
        LocalDateTime lastAutoReplyAt = senderUserId.equals(conversation.get("userAId"))
                ? toLocalDateTime(conversation.get("autoRepliedToAAt"))
                : toLocalDateTime(conversation.get("autoRepliedToBAt"));
        if (lastAutoReplyAt != null && lastAutoReplyAt.isAfter(originalMessageAt.minus(AUTO_REPLY_WINDOW))) {
            return;
        }

        LocalDateTime replyAt = originalMessageAt.plusNanos(1);
        Map<String, Object> autoMessage = new LinkedHashMap<>();
        autoMessage.put("conversationId", conversationId);
        autoMessage.put("senderUserId", autoReplyUserId);
        autoMessage.put("body", settings.getOrDefault("replyContent", DEFAULT_AUTO_REPLY));
        autoMessage.put("messageType", MESSAGE_TYPE_TEXT);
        autoMessage.put("mediaUrl", null);
        autoMessage.put("productId", null);
        autoMessage.put("orderId", null);
        autoMessage.put("isRead", false);
        autoMessage.put("readAt", null);
        autoMessage.put("createdAt", replyAt);
        messageMapper.insert(autoMessage);
        conversationMapper.incrementUnreadCount(conversationId, senderUserId);
        conversationMapper.updateAutoRepliedAt(conversationId, senderUserId, replyAt);
        conversationMapper.updateLastMessageAt(conversationId, replyAt.toString());
    }
}
