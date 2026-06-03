package com.youyu.backend.mapper.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findProductsPaged(String keyword, String status, String reviewStatus, String productType, int offset, int limit);

    long countProducts(String keyword, String status, String reviewStatus, String productType);

    long countAll();

    List<Map<String, Object>> findPublic();

    List<Map<String, Object>> findPublicByFilters(String keyword, Long categoryId, String productType, String sort);

    List<Map<String, Object>> findPublicByFiltersPaged(String keyword, Long categoryId, String productType, String sort, int offset, int limit);

    long countPublicByFilters(String keyword, Long categoryId, String productType);

    List<Map<String, Object>> findPublicByIds(List<Long> productIds);

    List<Map<String, Object>> findPublicSearchIndexDocuments();

    Optional<Map<String, Object>> findPublicSearchIndexDocumentById(Long productId);

    List<Map<String, Object>> findBySellerId(Long sellerUserId);

    List<Map<String, Object>> findByShopId(Long shopId);

    List<Map<String, Object>> findFavoritesByUserId(Long userId);

    Optional<Map<String, Object>> findById(Long id);

    List<Map<String, Object>> findMediaByProductId(Long productId);

    List<Map<String, Object>> findDigitalAssetsByProductId(Long productId);

    boolean isFavorite(Long userId, Long productId);

    void addFavorite(Long userId, Long productId);

    boolean removeFavorite(Long userId, Long productId);

    Long insert(Map<String, Object> product);

    void replaceMedia(Long productId, List<String> mediaUrls);

    void update(Long productId, Map<String, Object> product);

    void updateStatus(Long productId, String status);

    void updateFavoriteCount(Long productId, int delta);

    void updateReviewResult(Long productId, String reviewStatus, String status, String rejectReason);

    void softDelete(Long productId);
}
