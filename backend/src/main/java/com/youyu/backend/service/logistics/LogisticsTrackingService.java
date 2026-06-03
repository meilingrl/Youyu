package com.youyu.backend.service.logistics;

import java.util.Map;

public interface LogisticsTrackingService {

    Map<String, Object> buildTrackingPayload(Map<String, Object> order, Map<String, Object> fulfillment);

    Map<String, Object> buildMapPayload(Map<String, Object> tracking, Map<String, Object> fulfillment);
}
