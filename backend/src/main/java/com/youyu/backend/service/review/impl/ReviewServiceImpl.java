package com.youyu.backend.service.review.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.review.ReviewMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.service.review.ReviewService;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_IMAGE_COUNT = 3;
    private static final int MAX_IMAGE_DATA_URL_LENGTH = 7_000_000;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final ReviewMapper reviewMapper;
    private final TransactionDataStore transactionDataStore;
    private final ShopMapper shopMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             TransactionDataStore transactionDataStore,
                             ShopMapper shopMapper) {
        this.reviewMapper = reviewMapper;
        this.transactionDataStore = transactionDataStore;
        this.shopMapper = shopMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> submitProductReview(Long buyerUserId, Map<String, Object> command) {
        if (buyerUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        int score = requiredInt(command.get("score"), "score");
        assertScore(score);

        String content = optionalString(command.get("content"), "");
        assertContentLength(content);
        List<Map<String, Object>> images = normalizeImages(command.get("images"));

        Long orderItemId = requiredLong(command.get("orderItemId"), "orderItemId");

        Map<String, Object> orderContext = reviewMapper.findOrderContextByOrderItemId(orderItemId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "订单项不存在"));

        if (!Objects.equals(orderContext.get("buyerUserId"), buyerUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能评价自己的订单");
        }
        if (!"completed".equals(orderContext.get("orderStatus"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "只能评价已完成的订单");
        }

        Long productId = toLong(orderContext.get("productId"));

        Map<String, Object> reviewData = new LinkedHashMap<>();
        reviewData.put("orderItemId", orderItemId);
        reviewData.put("buyerUserId", buyerUserId);
        reviewData.put("productId", productId);
        reviewData.put("score", score);
        reviewData.put("content", content);

        Long reviewId;
        try {
            reviewId = reviewMapper.insertProductReview(reviewData);
            reviewMapper.insertReviewImages("product", reviewId, images);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "您已评价过该商品");
        }

        recalculateProductRating(productId);

        return reviewMapper.findProductReviewById(reviewId)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "评价创建失败"));
    }

    @Override
    @Transactional
    public Map<String, Object> submitShopReview(Long buyerUserId, Map<String, Object> command) {
        if (buyerUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }

        int score = requiredInt(command.get("score"), "score");
        assertScore(score);

        String content = optionalString(command.get("content"), "");
        assertContentLength(content);
        List<Map<String, Object>> images = normalizeImages(command.get("images"));

        Long shopId = requiredLong(command.get("shopId"), "shopId");

        Map<String, Object> shop = shopMapper.findById(shopId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "店铺不存在"));
        if (!"approved".equals(shop.get("reviewStatus"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该店铺暂不支持评价");
        }

        List<Map<String, Object>> buyerOrders = transactionDataStore.listOrdersForBuyer(buyerUserId);
        boolean hasCompletedOrderFromShop = buyerOrders.stream()
                .anyMatch(o -> "completed".equals(o.get("orderStatus"))
                        && Objects.equals(o.get("shopId"), shopId));
        if (!hasCompletedOrderFromShop) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "您需要在该店铺有已完成订单才能评价");
        }

        Map<String, Object> reviewData = new LinkedHashMap<>();
        reviewData.put("shopId", shopId);
        reviewData.put("buyerUserId", buyerUserId);
        reviewData.put("score", score);
        reviewData.put("content", content);

        Long reviewId;
        try {
            reviewId = reviewMapper.insertShopReview(reviewData);
            reviewMapper.insertReviewImages("shop", reviewId, images);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "您已评价过该店铺");
        }

        recalculateShopRating(shopId);

        return reviewMapper.findShopReviewById(reviewId)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "评价创建失败"));
    }

    @Override
    public Map<String, Object> getProductReviews(Long productId, int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> items = reviewMapper.findProductReviewsByProductId(productId, offset, pageSize);
        long total = reviewMapper.countProductReviewsByProductId(productId);

        return linkedMap("items", items, "total", total, "page", page, "pageSize", pageSize);
    }

    @Override
    public Map<String, Object> getShopReviews(Long shopId, int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> items = reviewMapper.findShopReviewsByShopId(shopId, offset, pageSize);
        long total = reviewMapper.countShopReviewsByShopId(shopId);

        return linkedMap("items", items, "total", total, "page", page, "pageSize", pageSize);
    }

    @Override
    public Map<String, Object> getProductReviewSummary(Long productId) {
        List<Map<String, Object>> agg = reviewMapper.summarizeProductRatings(productId);
        double avgScore = 0.0;
        long reviewCount = 0;
        if (!agg.isEmpty()) {
            Map<String, Object> row = agg.get(0);
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            reviewCount = row.get("count") instanceof Number n ? n.longValue() : 0;
        }
        avgScore = Math.round(avgScore * 100.0) / 100.0;

        List<Map<String, Object>> distribution =
                buildDistribution(reviewMapper.summarizeProductRatingDistribution(productId));

        return linkedMap("avgScore", avgScore, "reviewCount", reviewCount, "distribution", distribution);
    }

    @Override
    public Map<String, Object> getShopReviewSummary(Long shopId) {
        List<Map<String, Object>> agg = reviewMapper.summarizeShopRatings(shopId);
        double avgScore = 0.0;
        long reviewCount = 0;
        if (!agg.isEmpty()) {
            Map<String, Object> row = agg.get(0);
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            reviewCount = row.get("count") instanceof Number n ? n.longValue() : 0;
        }
        avgScore = Math.round(avgScore * 100.0) / 100.0;

        List<Map<String, Object>> distribution =
                buildDistribution(reviewMapper.summarizeShopRatingDistribution(shopId));

        return linkedMap("avgScore", avgScore, "reviewCount", reviewCount, "distribution", distribution);
    }

    @Override
    public Map<String, Object> getPendingReviewItems(Long buyerUserId) {
        if (buyerUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        List<Map<String, Object>> items = reviewMapper.findPendingReviewableOrderItems(buyerUserId);
        return linkedMap("items", items);
    }

    @Override
    public Map<String, Object> getMyReviews(Long buyerUserId) {
        if (buyerUserId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        List<Map<String, Object>> productReviews = reviewMapper.findMyProductReviews(buyerUserId);
        List<Map<String, Object>> shopReviews = reviewMapper.findMyShopReviews(buyerUserId);
        return linkedMap("productReviews", productReviews, "shopReviews", shopReviews);
    }

    private void recalculateProductRating(Long productId) {
        List<Map<String, Object>> agg = reviewMapper.summarizeProductRatings(productId);
        double avgScore = 0.0;
        int count = 0;
        if (!agg.isEmpty()) {
            Map<String, Object> row = agg.get(0);
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            count = row.get("count") instanceof Number n ? n.intValue() : 0;
        }
        avgScore = Math.round(avgScore * 100.0) / 100.0;
        reviewMapper.updateProductRating(productId, avgScore, count);
    }

    private void recalculateShopRating(Long shopId) {
        List<Map<String, Object>> agg = reviewMapper.summarizeShopRatings(shopId);
        double avgScore = 0.0;
        int count = 0;
        if (!agg.isEmpty()) {
            Map<String, Object> row = agg.get(0);
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            count = row.get("count") instanceof Number n ? n.intValue() : 0;
        }
        avgScore = Math.round(avgScore * 100.0) / 100.0;
        reviewMapper.updateShopRating(shopId, avgScore, count);
    }

    private int requiredInt(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " is required");
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " must be a number");
        }
    }

    private Long requiredLong(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " is required");
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " must be a number");
        }
    }

    private String optionalString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private void assertScore(int score) {
        if (score < 1 || score > 5) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评分必须在1到5之间");
        }
    }

    private void assertContentLength(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "评价内容不能超过" + MAX_CONTENT_LENGTH + "字");
        }
    }

    private List<Map<String, Object>> normalizeImages(Object value) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> rawImages)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "images must be an array");
        }
        if (rawImages.size() > MAX_IMAGE_COUNT) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "review images cannot exceed " + MAX_IMAGE_COUNT);
        }

        List<Map<String, Object>> images = new ArrayList<>();
        for (Object rawImage : rawImages) {
            if (!(rawImage instanceof Map<?, ?> rawMap)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "image metadata must be an object");
            }

            String mediaUrl = optionalString(rawMap.get("mediaUrl"), "").trim();
            String mimeType = optionalString(rawMap.get("mimeType"), "").trim().toLowerCase();
            String fileName = optionalString(rawMap.get("fileName"), "").trim();

            if (mediaUrl.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "image mediaUrl is required");
            }
            if (mediaUrl.length() > MAX_IMAGE_DATA_URL_LENGTH) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "review image is too large");
            }
            if (mimeType.isBlank()) {
                mimeType = inferDataUrlMimeType(mediaUrl);
            }
            if (!ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "unsupported review image type");
            }
            if (!mediaUrl.startsWith("data:" + mimeType + ";base64,")) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "review image must be a matching data URL");
            }

            Map<String, Object> image = new LinkedHashMap<>();
            image.put("mediaUrl", mediaUrl);
            image.put("mimeType", mimeType);
            image.put("fileName", fileName.length() > 255 ? fileName.substring(0, 255) : fileName);
            images.add(image);
        }
        return images;
    }

    private String inferDataUrlMimeType(String mediaUrl) {
        if (!mediaUrl.startsWith("data:")) {
            return "";
        }
        int separatorIndex = mediaUrl.indexOf(";base64,");
        if (separatorIndex < 0) {
            return "";
        }
        return mediaUrl.substring("data:".length(), separatorIndex).toLowerCase();
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private List<Map<String, Object>> buildDistribution(List<Map<String, Object>> groupedRows) {
        Map<Integer, Long> groupedCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : groupedRows) {
            Integer score = row.get("score") instanceof Number n ? n.intValue() : null;
            if (score == null) {
                continue;
            }
            long count = row.get("count") instanceof Number n ? n.longValue() : 0L;
            groupedCounts.put(score, count);
        }

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int score = 1; score <= 5; score++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("score", score);
            entry.put("count", groupedCounts.getOrDefault(score, 0L));
            distribution.add(entry);
        }
        return distribution;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> linkedMap(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
