package com.youyu.backend.controller.chat;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.chat.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/conversations/{id}/messages")
    @LoginRequired
    public ApiResponse<Map<String, Object>> sendMessage(
            @PathVariable Long id,
            @RequestBody SendMessageRequest req,
            HttpServletRequest request) {
        Long userId = AuthContextHolder.get().getUserId();
        return ApiResponse.success(chatService.sendMessage(id, userId, req.getBody()), traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
