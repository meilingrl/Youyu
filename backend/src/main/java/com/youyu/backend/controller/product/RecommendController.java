package com.youyu.backend.controller.product;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.product.RecommendService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/home")
    public ApiResponse<List<Map<String, Object>>> homeRecommend(
            @RequestParam(defaultValue = "8") int limit,
            HttpServletRequest request) {
        Long userId = currentUserId();
        return ApiResponse.success(
                recommendService.recommendForHome(limit, userId),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE));
    }

    @GetMapping("/also-bought/{productId}")
    public ApiResponse<List<Map<String, Object>>> alsoBought(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest request) {
        return ApiResponse.success(
                recommendService.recommendAlsoBought(productId, limit),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE));
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }
}
