package com.youyu.backend.mapper.review;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewMapper {

    Long insertProductReview(Map<String, Object> reviewData);

    void insertReviewImages(String reviewType, Long reviewId, List<Map<String, Object>> images);

    Optional<Map<String, Object>> findProductReviewById(Long id);

    List<Map<String, Object>> findProductReviewsByProductId(Long productId, int offset, int limit);

    long countProductReviewsByProductId(Long productId);

    List<Map<String, Object>> summarizeProductRatings(Long productId);

    List<Map<String, Object>> summarizeProductRatingDistribution(Long productId);

    Long insertShopReview(Map<String, Object> reviewData);

    Optional<Map<String, Object>> findShopReviewById(Long id);

    List<Map<String, Object>> findShopReviewsByShopId(Long shopId, int offset, int limit);

    long countShopReviewsByShopId(Long shopId);

    List<Map<String, Object>> summarizeShopRatings(Long shopId);

    List<Map<String, Object>> summarizeShopRatingDistribution(Long shopId);

    void updateProductRating(Long productId, double avgScore, int count);

    void updateShopRating(Long shopId, double avgScore, int count);

    List<Map<String, Object>> findPendingReviewableOrderItems(Long buyerUserId);

    List<Map<String, Object>> findMyProductReviews(Long buyerUserId);

    List<Map<String, Object>> findMyShopReviews(Long buyerUserId);

    Optional<Map<String, Object>> findOrderContextByOrderItemId(Long orderItemId);
}
