package com.campusmarket.backend.mapper.recommend;

import java.util.List;
import java.util.Map;

public interface RecommendMapper {

    List<Map<String, Object>> findPopularProducts(int limit);

    List<Map<String, Object>> findPopularByCategoryIds(List<Long> categoryIds, int limit);

    List<Map<String, Object>> findCoPurchased(Long productId, int limit);
}
