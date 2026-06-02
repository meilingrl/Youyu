package com.youyu.backend.mapper.shop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ShopMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findShopsPaged(String keyword, String status, String reviewStatus, int offset, int limit);

    long countShops(String keyword, String status, String reviewStatus);

    long countAll();

    Optional<Map<String, Object>> findById(Long id);

    Optional<Map<String, Object>> findByOwnerUserId(Long ownerUserId);

    Optional<Map<String, Object>> findCapabilityByShopId(Long shopId);

    Long insertApplication(Map<String, Object> shop);

    void updateStatus(Long shopId, String status, String reviewStatus, Long reviewerId, String rejectReason);

    Map<String, Object> summarizeMonthlyInsight(Long shopId, LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<Map<String, Object>> findHotProducts(Long shopId, LocalDateTime startInclusive, LocalDateTime endExclusive, int limit);

    List<Map<String, Object>> summarizeCompletedSalesByCategory(int limit);

    List<Map<String, Object>> rankShopsByCompletedSales(int limit);
}
