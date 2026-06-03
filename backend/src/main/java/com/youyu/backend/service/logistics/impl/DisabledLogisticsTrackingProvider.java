package com.youyu.backend.service.logistics.impl;

import com.youyu.backend.service.logistics.LogisticsTrackingProvider;
import com.youyu.backend.service.logistics.LogisticsTrackingRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DisabledLogisticsTrackingProvider implements LogisticsTrackingProvider {

    @Override
    public Map<String, Object> track(LogisticsTrackingRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "disabled");
        result.put("status", "disabled");
        result.put("message", "Logistics tracking provider is disabled or not configured.");
        result.put("events", List.of());
        return result;
    }
}
