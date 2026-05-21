package com.campusmarket.backend.service.review.impl;

import com.campusmarket.backend.common.api.ResultCode;
import com.campusmarket.backend.common.exception.BusinessException;
import com.campusmarket.backend.mapper.review.ReviewMapper;
import com.campusmarket.backend.mapper.shop.ShopMapper;
import com.campusmarket.backend.service.review.ReviewService;
import com.campusmarket.backend.service.transaction.support.TransactionDataStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_PAGE_SIZE = 50;

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

        Long orderItemId = requiredLong(command.get("orderItemId"), "orderItemId");

        // Resolve order context from order item in a single bounded query
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
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "您已评价过该商品");
        }

        // 重新聚合并持久化评分到 products 表的冗余字段（rating_score / review_count），
        // 避免每次商品列表/详情读取时执行 AVG / COUNT 聚合查询。
        // 注意：此操作在事务内执行，并发提交评价时存在短暂的读写竞争窗口。
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

        Long shopId = requiredLong(command.get("shopId"), "shopId");

        // Verify shop exists and is approved
        Map<String, Object> shop = shopMapper.findById(shopId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "店铺不存在"));
        if (!"approved".equals(shop.get("reviewStatus"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该店铺暂不支持评价");
        }

        // Verify buyer has at least one completed order from this shop
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
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "您已评价过该店铺");
        }

        // 同上：将店铺评分聚合并写入 shops 表冗余字段，以读优化换写开销。
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
            // 防御性解包：JDBC queryForList 返回原始 Object，可能是 BigDecimal、Double、Long 或 null。
            // Mapper 层 SQL 已使用 COALESCE(AVG(score), 0.0)，此处是双重保险。
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            reviewCount = row.get("count") instanceof Number n ? n.longValue() : 0;
        }
        avgScore = Math.round(avgScore * 100.0) / 100.0;

        // TODO: 用真实 GROUP BY 查询替换 stub distribution：
        //   SELECT score, COUNT(*) FROM reviews WHERE product_id = ? GROUP BY score
        // 当前返回 5 个评分级别全 0，作为 MVP 占位。
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("score", s);
            entry.put("count", 0L);
            distribution.add(entry);
        }

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

        // TODO: 用真实 GROUP BY 查询替换 stub distribution：
        //   SELECT score, COUNT(*) FROM shop_reviews WHERE shop_id = ? GROUP BY score
        // 当前返回 5 个评分级别全 0，作为 MVP 占位。
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("score", s);
            entry.put("count", 0L);
            distribution.add(entry);
        }

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

    // ── Private helpers ──

    /**
     * 重新聚合所有商品评价分数，更新 products 表的冗余评分字段。
     * avgScore 四舍五入到 2 位小数以避免浮点 artifacts（如 4.299999999）。
     * 防御性的 instanceof Number 类型检查兼容 JDBC 驱动返回 BigDecimal / Double / null 的差异。
     */
    private void recalculateProductRating(Long productId) {
        List<Map<String, Object>> agg = reviewMapper.summarizeProductRatings(productId);
        double avgScore = 0.0;
        int count = 0;
        if (!agg.isEmpty()) {
            Map<String, Object> row = agg.get(0);
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            count = row.get("count") instanceof Number n ? ((Number) row.get("count")).intValue() : 0;
        }
        avgScore = Math.round(avgScore * 100.0) / 100.0;
        reviewMapper.updateProductRating(productId, avgScore, count);
    }

    /**
     * 同上：重新聚合店铺评分，更新 shops 表冗余字段。
     */
    private void recalculateShopRating(Long shopId) {
        List<Map<String, Object>> agg = reviewMapper.summarizeShopRatings(shopId);
        double avgScore = 0.0;
        int count = 0;
        if (!agg.isEmpty()) {
            Map<String, Object> row = agg.get(0);
            avgScore = row.get("avgScore") instanceof Number n ? n.doubleValue() : 0.0;
            count = row.get("count") instanceof Number n ? ((Number) row.get("count")).intValue() : 0;
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
        if (value == null) return fallback;
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

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return null;
        return Long.parseLong(String.valueOf(value));
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
