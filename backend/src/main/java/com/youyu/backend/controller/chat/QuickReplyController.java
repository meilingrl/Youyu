package com.youyu.backend.controller.chat;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.chat.QuickReplyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/quick-replies")
@LoginRequired
public class QuickReplyController {

    private final QuickReplyService quickReplyService;

    public QuickReplyController(QuickReplyService quickReplyService) {
        this.quickReplyService = quickReplyService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.success(quickReplyService.listQuickReplies(currentUserId()), traceId(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody QuickReplyRequest req, HttpServletRequest request) {
        return ApiResponse.success(
                quickReplyService.createQuickReply(currentUserId(), req.getContent(), req.getSortOrder()),
                traceId(request)
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody QuickReplyRequest req,
                                    HttpServletRequest request) {
        quickReplyService.updateQuickReply(currentUserId(), id, req.getContent(), req.getSortOrder());
        return ApiResponse.success(null, traceId(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        quickReplyService.deleteQuickReply(currentUserId(), id);
        return ApiResponse.success(null, traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    public static class QuickReplyRequest {
        private String content;
        private Integer sortOrder;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
