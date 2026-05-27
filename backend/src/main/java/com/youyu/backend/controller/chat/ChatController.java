package com.youyu.backend.controller.chat;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.chat.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    @LoginRequired
    public ApiResponse<Map<String, Object>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.getConversations(userId, page, size), traceId(request));
    }

    @GetMapping("/unread-count")
    @LoginRequired
    public ApiResponse<Map<String, Object>> getUnreadCount(HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.getUnreadCount(userId), traceId(request));
    }

    @PostMapping("/conversations")
    @LoginRequired
    public ApiResponse<Map<String, Object>> createConversation(@RequestBody CreateConversationRequest req,
                                                                HttpServletRequest request) {
        Long currentUserId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.findOrCreateConversation(
                currentUserId, req.getPeerUserId(), req.getProductId(), req.getShopId()), traceId(request));
    }

    @GetMapping("/conversations/{id}/messages")
    @LoginRequired
    public ApiResponse<Map<String, Object>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.getMessages(id, userId, page, size), traceId(request));
    }

    @GetMapping("/messages/search")
    @LoginRequired
    public ApiResponse<Map<String, Object>> searchMessages(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.searchMessages(userId, keyword, startTime, endTime, page, size), traceId(request));
    }

    @PostMapping("/conversations/{id}/messages")
    @LoginRequired
    public ApiResponse<Map<String, Object>> sendMessage(
            @PathVariable Long id,
            @RequestBody SendMessageRequest req,
            HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.sendMessage(
                id,
                userId,
                req.getBody(),
                req.getMessageType(),
                req.getMediaUrl(),
                req.getProductId(),
                req.getOrderId()), traceId(request));
    }

    @PostMapping("/conversations/{id}/read")
    @LoginRequired
    public ApiResponse<Void> markConversationRead(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        chatService.markConversationRead(id, userId);
        return ApiResponse.success(null, traceId(request));
    }

    @PostMapping("/conversations/{id}/pin")
    @LoginRequired
    public ApiResponse<Void> updatePin(
            @PathVariable Long id,
            @RequestBody ConversationFlagRequest req,
            HttpServletRequest request) {
        chatService.updatePinStatus(id, AuthContextHolder.get().getUserId(), req.isPinned());
        return ApiResponse.success(null, traceId(request));
    }

    @PostMapping("/conversations/{id}/mute")
    @LoginRequired
    public ApiResponse<Void> updateMute(
            @PathVariable Long id,
            @RequestBody ConversationFlagRequest req,
            HttpServletRequest request) {
        chatService.updateMuteStatus(id, AuthContextHolder.get().getUserId(), req.isMuted());
        return ApiResponse.success(null, traceId(request));
    }

    @DeleteMapping("/conversations/{id}")
    @LoginRequired
    public ApiResponse<Void> deleteConversation(
            @PathVariable Long id,
            HttpServletRequest request) {
        chatService.deleteConversation(id, AuthContextHolder.get().getUserId());
        return ApiResponse.success(null, traceId(request));
    }

    @PostMapping("/messages/{id}/recall")
    @LoginRequired
    public ApiResponse<Void> recallMessage(
            @PathVariable Long id,
            HttpServletRequest request) {
        chatService.recallMessage(id, AuthContextHolder.get().getUserId());
        return ApiResponse.success(null, traceId(request));
    }

    @GetMapping("/auto-reply")
    @LoginRequired
    public ApiResponse<Map<String, Object>> getAutoReply(HttpServletRequest request) {
        return ApiResponse.success(chatService.getAutoReplySettings(AuthContextHolder.get().getUserId()), traceId(request));
    }

    @PutMapping("/auto-reply")
    @LoginRequired
    public ApiResponse<Void> updateAutoReply(@RequestBody AutoReplyRequest req, HttpServletRequest request) {
        chatService.updateAutoReplySettings(AuthContextHolder.get().getUserId(), req.getIsEnabled(), req.getReplyContent());
        return ApiResponse.success(null, traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    public static class ConversationFlagRequest {
        private boolean pinned;
        private boolean muted;

        public boolean isPinned() {
            return pinned;
        }

        public void setPinned(boolean pinned) {
            this.pinned = pinned;
        }

        public boolean isMuted() {
            return muted;
        }

        public void setMuted(boolean muted) {
            this.muted = muted;
        }
    }

    public static class AutoReplyRequest {
        private Boolean isEnabled;
        private String replyContent;

        public Boolean getIsEnabled() {
            return isEnabled;
        }

        public void setIsEnabled(Boolean isEnabled) {
            this.isEnabled = isEnabled;
        }

        public String getReplyContent() {
            return replyContent;
        }

        public void setReplyContent(String replyContent) {
            this.replyContent = replyContent;
        }
    }
}
