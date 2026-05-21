package com.campusmarket.backend.controller.shop;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.auth.AuthContextHolder;
import com.campusmarket.backend.common.auth.LoginRequired;
import com.campusmarket.backend.common.auth.UserRole;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.service.review.ReviewService;
import com.campusmarket.backend.service.shop.ShopService;
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
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;
    private final ReviewService reviewService;

    public ShopController(ShopService shopService, ReviewService reviewService) {
        this.shopService = shopService;
        this.reviewService = reviewService;
    }

    @GetMapping("/skeleton")
    public ApiResponse<Map<String, Object>> skeleton(HttpServletRequest request) {
        return ApiResponse.success(
                shopService.moduleInfo(),
                traceId(request)
        );
    }

    @GetMapping("/mine")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> mine(HttpServletRequest request) {
        return ApiResponse.success(shopService.getMyShop(currentUserId()), traceId(request));
    }

    @PostMapping("/applications")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> apply(@RequestBody Map<String, Object> command,
                                                  HttpServletRequest request) {
        return ApiResponse.success(shopService.applyShop(currentUserId(), command), traceId(request));
    }

    @GetMapping("/{shopId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long shopId, HttpServletRequest request) {
        return ApiResponse.success(shopService.getShopDetail(shopId), traceId(request));
    }

    @GetMapping("/{shopId}/insight-snapshot")
    public ApiResponse<Map<String, Object>> insightSnapshot(@PathVariable Long shopId,
                                                            HttpServletRequest request) {
        return ApiResponse.success(
                shopService.insightSnapshot(shopId),
                traceId(request)
        );
    }

    @GetMapping("/{shopId}/reviews")
    public ApiResponse<Map<String, Object>> reviews(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(reviewService.getShopReviews(shopId, page, pageSize), traceId(request));
    }

    @GetMapping("/{shopId}/review-summary")
    public ApiResponse<Map<String, Object>> reviewSummary(
            @PathVariable Long shopId,
            HttpServletRequest request) {
        return ApiResponse.success(reviewService.getShopReviewSummary(shopId), traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }
}
