package com.campusmarket.backend.mapper.order;

import java.util.List;
import java.util.Map;

public interface DigitalAccessMapper {

    Long insert(Long orderId, Long userId, Long assetId, String assetName, String accessType);

    List<Map<String, Object>> findByOrderId(Long orderId);
}
