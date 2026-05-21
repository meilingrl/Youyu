package com.campusmarket.backend.controller.review;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.auth.AuthContextHolder;
import com.campusmarket.backend.common.auth.LoginRequired;
import com.campusmarket.backend.common.auth.UserRole;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.service.review.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> submitProductReview(
            @RequestBody Map<String, Object> command,
            HttpServletRequest request) {
        return ApiResponse.success(
                reviewService.submitProductReview(currentUserId(), command),
                traceId(request));
    }

    @PostMapping("/shops")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> submitShopReview(
            @RequestBody Map<String, Object> command,
            HttpServletRequest request) {
        return ApiResponse.success(
                reviewService.submitShopReview(currentUserId(), command),
                traceId(request));
    }

    @GetMapping("/pending")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> pending(HttpServletRequest request) {
        return ApiResponse.success(
                reviewService.getPendingReviewItems(currentUserId()),
                traceId(request));
    }

    @GetMapping("/mine")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> mine(HttpServletRequest request) {
        return ApiResponse.success(
                reviewService.getMyReviews(currentUserId()),
                traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
