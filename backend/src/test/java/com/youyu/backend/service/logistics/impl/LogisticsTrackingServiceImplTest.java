package com.youyu.backend.service.logistics.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.config.LogisticsIntegrationProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogisticsTrackingServiceImplTest {

    @Test
    void buildsDeliveryAddressMarkerForHunnanCampusWhenTrackingCoordinatesAreUnavailable() {
        LogisticsIntegrationProperties properties = new LogisticsIntegrationProperties();
        properties.getAmap().setEnabled(true);
        properties.getAmap().setWebServiceKey("test-web-service-key");
        LogisticsTrackingServiceImpl service = new LogisticsTrackingServiceImpl(properties, new ObjectMapper());

        Map<String, Object> tracking = Map.of(
                "status", "missing_tracking_no",
                "events", List.of()
        );
        Map<String, Object> fulfillment = Map.of(
                "addressSnapshot", Map.of(
                        "province", "辽宁省",
                        "city", "沈阳市",
                        "district", "浑南区",
                        "campusName", "东北大学浑南校区",
                        "detailAddress", "宿舍1号楼"
                )
        );

        Map<String, Object> payload = service.buildMapPayload(tracking, fulfillment);

        assertThat(payload.get("status")).isEqualTo("ready");
        assertThat(payload.get("source")).isEqualTo("delivery_address");
        List<?> markers = (List<?>) payload.get("markers");
        assertThat(markers).hasSize(1);
        Map<?, ?> marker = (Map<?, ?>) markers.get(0);
        assertThat(marker.get("locationText")).asString().contains("东北大学浑南校区");
        assertThat(marker.get("approximate")).isEqualTo(true);
        Map<?, ?> coordinates = (Map<?, ?>) marker.get("coordinates");
        assertThat(coordinates.get("lng")).isEqualTo("123.4260");
        assertThat(coordinates.get("lat")).isEqualTo("41.6564");
    }
}
