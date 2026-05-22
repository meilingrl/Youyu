package com.youyu.backend.service.product.impl;

import com.youyu.backend.mapper.recommend.RecommendMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.product.RecommendService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecommendServiceImpl implements RecommendService {

    private final RecommendMapper recommendMapper;
    private final UserMapper userMapper;

    public RecommendServiceImpl(RecommendMapper recommendMapper, UserMapper userMapper) {
        this.recommendMapper = recommendMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> recommendForHome(int limit, Long userId) {
        int effectiveLimit = Math.max(1, Math.min(limit, 50));

        if (userId == null) {
            return recommendMapper.findPopularProducts(effectiveLimit);
        }

        List<Map<String, Object>> purchasedCategories = userMapper.summarizePurchasedCategories(userId, 5);
        long totalItems = purchasedCategories.stream()
                .mapToLong(c -> ((Number) c.getOrDefault("ITEM_COUNT", 0)).longValue())
                .sum();

        if (totalItems == 0 || purchasedCategories.isEmpty()) {
            return recommendMapper.findPopularProducts(effectiveLimit);
        }

        List<Long> categoryIds = purchasedCategories.stream()
                .map(c -> ((Number) c.get("CATEGORY_ID")).longValue())
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> personalized = recommendMapper.findPopularByCategoryIds(categoryIds, effectiveLimit);

        for (Map<String, Object> item : personalized) {
            Long categoryId = ((Number) item.get("categoryId")).longValue();
            String categoryName = (String) item.get("categoryName");
            purchasedCategories.stream()
                    .filter(pc -> ((Number) pc.get("CATEGORY_ID")).longValue() == categoryId)
                    .findFirst()
                    .ifPresent(pc -> item.put("reason", "你曾购买过" + (categoryName != null ? categoryName : "相关商品")));
        }

        if (personalized.size() < effectiveLimit) {
            List<Map<String, Object>> popular = recommendMapper.findPopularProducts(effectiveLimit);
            Set<Long> existingIds = personalized.stream()
                    .map(p -> ((Number) p.get("id")).longValue())
                    .collect(Collectors.toSet());
            for (Map<String, Object> item : popular) {
                if (personalized.size() >= effectiveLimit) break;
                Long id = ((Number) item.get("id")).longValue();
                if (!existingIds.contains(id)) {
                    personalized.add(item);
                    existingIds.add(id);
                }
            }
        }

        return personalized;
    }

    @Override
    public List<Map<String, Object>> recommendAlsoBought(Long productId, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 20));
        return recommendMapper.findCoPurchased(productId, effectiveLimit);
    }
}
