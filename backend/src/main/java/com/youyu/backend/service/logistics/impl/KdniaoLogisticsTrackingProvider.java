package com.youyu.backend.service.logistics.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.config.LogisticsIntegrationProperties;
import com.youyu.backend.service.logistics.LogisticsTrackingProvider;
import com.youyu.backend.service.logistics.LogisticsTrackingRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class KdniaoLogisticsTrackingProvider implements LogisticsTrackingProvider {

    private final LogisticsIntegrationProperties.Tracking properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public KdniaoLogisticsTrackingProvider(LogisticsIntegrationProperties.Tracking properties,
                                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getKdniaoEndpoint())
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    @Override
    public Map<String, Object> track(LogisticsTrackingRequest request) {
        try {
            String shipperCode = properties.resolveCarrierCode(request.logisticsCompany());
            if (shipperCode.isBlank()) {
                return unavailable("Carrier code is missing for the logistics company.");
            }
            String requestData = objectMapper.writeValueAsString(Map.of(
                    "OrderCode", "",
                    "ShipperCode", shipperCode,
                    "LogisticCode", request.trackingNo()
            ));
            String body = formBody(requestData);
            String rawResponse = restClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            Map<String, Object> payload = parseResponse(rawResponse);
            payload.put("provider", "kdniao");
            return payload;
        } catch (RuntimeException exception) {
            return failed(exception.getMessage());
        } catch (Exception exception) {
            return failed(exception.getMessage());
        }
    }

    private Map<String, Object> parseResponse(String rawResponse) throws Exception {
        Map<String, Object> raw = objectMapper.readValue(rawResponse, Map.class);
        boolean success = Boolean.TRUE.equals(raw.get("Success"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", success ? "available" : "unavailable");
        result.put("message", success ? "Provider returned logistics events." : String.valueOf(raw.getOrDefault("Reason", "Provider did not return events.")));
        result.put("state", raw.get("State"));
        result.put("events", normalizeEvents(raw.get("Traces")));
        return result;
    }

    private List<Map<String, Object>> normalizeEvents(Object traces) {
        if (!(traces instanceof List<?> traceList)) {
            return List.of();
        }
        return traceList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(trace -> {
                    Map<String, Object> event = new LinkedHashMap<>();
                    event.put("eventTime", trace.get("AcceptTime"));
                    event.put("statusText", trace.get("AcceptStation"));
                    event.put("locationText", trace.getOrDefault("Location", ""));
                    event.put("coordinates", null);
                    return event;
                })
                .toList();
    }

    private Map<String, Object> unavailable(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "kdniao");
        result.put("status", "unavailable");
        result.put("message", message);
        result.put("events", List.of());
        return result;
    }

    private Map<String, Object> failed(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "kdniao");
        result.put("status", "failed");
        result.put("message", message == null || message.isBlank() ? "Logistics provider request failed." : message);
        result.put("events", List.of());
        return result;
    }

    private String formBody(String requestData) throws Exception {
        return "RequestData=" + encode(requestData)
                + "&EBusinessID=" + encode(properties.getKdniaoBusinessId())
                + "&RequestType=1002"
                + "&DataSign=" + encode(sign(requestData))
                + "&DataType=2";
    }

    private String sign(String requestData) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] digest = md5.digest((requestData + properties.getKdniaoAppKey()).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private org.springframework.http.client.SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        return factory;
    }
}
