package com.youyu.backend.service.logistics;

public record LogisticsTrackingRequest(
        Long orderId,
        String trackingNo,
        String logisticsCompany
) {
}
