package com.youyu.backend.service.review;

import java.util.Map;

public interface ReviewService {

    Map<String, Object> submitProductReview(Long buyerUserId, Map<String, Object> command);

    Map<String, Object> submitShopReview(Long buyerUserId, Map<String, Object> command);

    Map<String, Object> getProductReviews(Long productId, int page, int pageSize);

    Map<String, Object> getShopReviews(Long shopId, int page, int pageSize);

    Map<String, Object> getProductReviewSummary(Long productId);

    Map<String, Object> getShopReviewSummary(Long shopId);

    Map<String, Object> getPendingReviewItems(Long buyerUserId);

    Map<String, Object> getMyReviews(Long buyerUserId);
}
