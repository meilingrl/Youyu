package com.youyu.backend.service.logistics.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.config.LogisticsIntegrationProperties;
import com.youyu.backend.service.logistics.LogisticsTrackingProvider;
import com.youyu.backend.service.logistics.LogisticsTrackingRequest;
import com.youyu.backend.service.logistics.LogisticsTrackingService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LogisticsTrackingServiceImpl implements LogisticsTrackingService {

    private final LogisticsIntegrationProperties properties;
    private final ObjectMapper objectMapper;
    private final LogisticsTrackingProvider disabledProvider = new DisabledLogisticsTrackingProvider();
    private final LogisticsTrackingProvider kdniaoProvider;
    private final RestClient amapRestClient;

    public LogisticsTrackingServiceImpl(LogisticsIntegrationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.kdniaoProvider = new KdniaoLogisticsTrackingProvider(properties.getTracking(), objectMapper);
        this.amapRestClient = RestClient.builder()
                .baseUrl("https://restapi.amap.com")
                .build();
    }

    @Override
    public Map<String, Object> buildTrackingPayload(Map<String, Object> order, Map<String, Object> fulfillment) {
        if (!"logistics".equals(order.get("fulfillmentType"))) {
            return null;
        }
        String trackingNo = value(fulfillment, "trackingNo");
        String logisticsCompany = value(fulfillment, "logisticsCompany");

        Map<String, Object> tracking = new LinkedHashMap<>();
        tracking.put("trackingNo", trackingNo);
        tracking.put("logisticsCompany", logisticsCompany);
        tracking.put("events", List.of());
        tracking.put("provider", providerName());
        tracking.put("status", "missing_tracking_no");
        tracking.put("message", "No logistics tracking number has been recorded for this order.");

        if (!trackingNo.isBlank()) {
            Map<String, Object> providerPayload = selectProvider().track(new LogisticsTrackingRequest(
                    castLong(order.get("id")),
                    trackingNo,
                    logisticsCompany
            ));
            tracking.putAll(providerPayload);
            tracking.put("trackingNo", trackingNo);
            tracking.put("logisticsCompany", logisticsCompany);
        }

        return tracking;
    }

    public Map<String, Object> buildMapPayload(Map<String, Object> tracking, Map<String, Object> fulfillment) {
        if (tracking == null) {
            return null;
        }
        List<Map<String, Object>> markers = eventsWithCoordinates(tracking);
        String source = "provider_events";
        if (markers.isEmpty()) {
            Map<String, Object> addressMarker = geocodeAddressMarker(fulfillment);
            if (!addressMarker.isEmpty()) {
                markers = List.of(addressMarker);
                source = "delivery_address";
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "amap");
        result.put("configured", properties.getAmap().isConfigured());
        result.put("status", resolveMapStatus(tracking, markers, source));
        result.put("message", resolveMapMessage(tracking, markers, source));
        result.put("markers", markers);
        result.put("polyline", "provider_events".equals(source) && markers.size() > 1 ? markers : List.of());
        result.put("source", source);
        return result;
    }

    private LogisticsTrackingProvider selectProvider() {
        return properties.getTracking().isKdniaoConfigured() ? kdniaoProvider : disabledProvider;
    }

    private String providerName() {
        return properties.getTracking().isKdniaoConfigured() ? "kdniao" : "disabled";
    }

    private String resolveMapStatus(Map<String, Object> tracking, List<Map<String, Object>> markers, String source) {
        if (!properties.getAmap().isConfigured()) {
            return "map_provider_disabled";
        }
        if (!markers.isEmpty()) {
            return "ready";
        }
        if ("available".equals(String.valueOf(tracking.get("status")))) {
            return "no_coordinates";
        }
        return "tracking_unavailable";
    }

    private String resolveMapMessage(Map<String, Object> tracking, List<Map<String, Object>> markers, String source) {
        if (!properties.getAmap().isConfigured()) {
            return "Amap WebService key is not configured on the backend.";
        }
        if (!markers.isEmpty() && "delivery_address".equals(source)) {
            return "Amap geocoded the delivery address. This shows the destination point, not a courier route.";
        }
        if (!markers.isEmpty()) {
            return "Map markers come from provider-derived logistics event coordinates.";
        }
        if (!"available".equals(String.valueOf(tracking.get("status")))) {
            return "Logistics tracking events are unavailable and no delivery address marker could be resolved.";
        }
        return "Provider events do not include coordinates. The frontend can still show the event timeline.";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> eventsWithCoordinates(Map<String, Object> tracking) {
        Object events = tracking.get("events");
        if (!(events instanceof List<?> eventList)) {
            return List.of();
        }
        return eventList.stream()
                .filter(Map.class::isInstance)
                .map(event -> (Map<String, Object>) event)
                .filter(event -> event.get("coordinates") instanceof Map<?, ?>)
                .map(event -> {
                    Map<String, Object> marker = new LinkedHashMap<>();
                    marker.put("title", event.get("statusText"));
                    marker.put("eventTime", event.get("eventTime"));
                    marker.put("locationText", event.get("locationText"));
                    marker.put("coordinates", event.get("coordinates"));
                    return marker;
                })
                .toList();
    }

    private Map<String, Object> geocodeAddressMarker(Map<String, Object> fulfillment) {
        if (!properties.getAmap().isConfigured()) {
            return Map.of();
        }
        String addressText = addressText(fulfillment);
        if (addressText.isBlank()) {
            return Map.of();
        }
        try {
            String raw = amapRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v3/geocode/geo")
                            .queryParam("key", properties.getAmap().getWebServiceKey())
                            .queryParam("address", addressText)
                            .build())
                    .retrieve()
                    .body(String.class);
            Map<String, Object> payload = objectMapper.readValue(raw, Map.class);
            Object geocodes = payload.get("geocodes");
            if (!(geocodes instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> first)) {
                return Map.of();
            }
            Object location = first.get("location");
            if (location == null || String.valueOf(location).isBlank()) {
                return Map.of();
            }
            String[] parts = String.valueOf(location).split(",", 2);
            if (parts.length != 2) {
                return Map.of();
            }
            Map<String, Object> coordinates = new LinkedHashMap<>();
            coordinates.put("lng", parts[0]);
            coordinates.put("lat", parts[1]);
            Map<String, Object> marker = new LinkedHashMap<>();
            marker.put("title", "Delivery address");
            marker.put("eventTime", "");
            marker.put("locationText", addressText);
            marker.put("coordinates", coordinates);
            return marker;
        } catch (RuntimeException exception) {
            return Map.of();
        } catch (Exception exception) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String addressText(Map<String, Object> fulfillment) {
        Object snapshot = fulfillment == null ? null : fulfillment.get("addressSnapshot");
        if (!(snapshot instanceof Map<?, ?> address)) {
            return "";
        }
        return List.of("province", "city", "district", "campusName", "detailAddress").stream()
                .map(key -> address.get(key))
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(String::valueOf)
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long castLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
