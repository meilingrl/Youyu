package com.campusmarket.backend.controller.product;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.auth.AuthContextHolder;
import com.campusmarket.backend.common.auth.LoginRequired;
import com.campusmarket.backend.common.auth.UserRole;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.service.product.ProductService;
import com.campusmarket.backend.service.review.ReviewService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    public ProductController(ProductService productService, ReviewService reviewService) {
        this.productService = productService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) String productType,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "12") int pageSize,
                                                  HttpServletRequest request) {
        return ApiResponse.success(
                productService.listProducts(keyword, categoryId, productType, currentUserId(), page, pageSize),
                traceId(request));
    }

    @GetMapping("/{productId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long productId, HttpServletRequest request) {
        return ApiResponse.success(productService.getProductDetail(productId), traceId(request));
    }

    @GetMapping("/mine")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<List<Map<String, Object>>> mine(HttpServletRequest request) {
        return ApiResponse.success(productService.listMyProducts(currentUserId()), traceId(request));
    }

    @PostMapping
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> publish(@RequestBody Map<String, Object> command,
                                                    HttpServletRequest request) {
        return ApiResponse.success(productService.publishProduct(currentUserId(), command), traceId(request));
    }

    @PutMapping("/{productId}")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> update(@PathVariable Long productId,
                                                   @RequestBody Map<String, Object> command,
                                                   HttpServletRequest request) {
        return ApiResponse.success(productService.updateProduct(currentUserId(), productId, command), traceId(request));
    }

    @PutMapping("/{productId}/status")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long productId,
                                                         @RequestBody Map<String, String> payload,
                                                         HttpServletRequest request) {
        return ApiResponse.success(
                productService.updateProductStatus(currentUserId(), productId, payload.getOrDefault("status", "")),
                traceId(request));
    }

    @DeleteMapping("/{productId}")
    @LoginRequired(roles = {UserRole.USER})
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long productId, HttpServletRequest request) {
        return ApiResponse.success(productService.deleteProduct(currentUserId(), productId), traceId(request));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<Map<String, Object>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(reviewService.getProductReviews(id, page, pageSize), traceId(request));
    }

    @GetMapping("/{id}/review-summary")
    public ApiResponse<Map<String, Object>> reviewSummary(
            @PathVariable Long id,
            HttpServletRequest request) {
        return ApiResponse.success(reviewService.getProductReviewSummary(id), traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }

    private Long currentUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }
}
