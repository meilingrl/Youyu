package com.youyu.backend.service.logistics;

import java.util.Map;

public interface LogisticsTrackingProvider {

    Map<String, Object> track(LogisticsTrackingRequest request);
}
