package com.youyu.backend.service.product;

import java.util.List;
import java.util.Map;

public interface RecommendService {

    List<Map<String, Object>> recommendForHome(int limit, Long userId);

    List<Map<String, Object>> recommendAlsoBought(Long productId, int limit);

    void invalidateRecommendationCaches();
}
