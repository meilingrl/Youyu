package com.youyu.backend.service.search.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.config.MeilisearchProperties;
import com.youyu.backend.service.search.ProductSearchCriteria;
import com.youyu.backend.service.search.ProductSearchIndex;
import com.youyu.backend.service.search.ProductSearchResult;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MeilisearchProductSearchIndex implements ProductSearchIndex {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchProductSearchIndex.class);

    private static final Map<String, List<String>> SORTS = Map.of(
            "newest", List.of("createdAt:desc", "productId:desc"),
            "price_asc", List.of("salePrice:asc", "createdAt:desc", "productId:desc"),
            "price_desc", List.of("salePrice:desc", "createdAt:desc", "productId:desc"),
            "sales_desc", List.of("favoriteCount:desc", "viewCount:desc", "createdAt:desc", "productId:desc")
    );

    private final MeilisearchProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MeilisearchProductSearchIndex(MeilisearchProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(normalizedHost(properties.getHost()))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
                        request.getHeaders().setBearerAuth(properties.getApiKey());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    @Override
    public Optional<ProductSearchResult> search(ProductSearchCriteria criteria) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("q", criteria.keyword() == null ? "" : criteria.keyword().trim());
            payload.put("offset", Math.max(0, (criteria.page() - 1) * criteria.pageSize()));
            payload.put("limit", criteria.pageSize());
            List<String> filters = filters(criteria);
            if (!filters.isEmpty()) {
                payload.put("filter", filters);
            }
            payload.put("sort", SORTS.getOrDefault(criteria.sort(), SORTS.get("newest")));

            Map<String, Object> response = post(indexPath() + "/search", payload);
            List<Long> productIds = hits(response).stream()
                    .map(hit -> toLong(hit.get("productId")))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
            long total = toLong(response.get("estimatedTotalHits"))
                    .or(() -> toLong(response.get("totalHits")))
                    .orElse((long) productIds.size());
            return Optional.of(new ProductSearchResult(productIds, total));
        } catch (Exception e) {
            log.warn("Meilisearch product search unavailable; falling back to MySQL", e);
            return Optional.empty();
        }
    }

    @Override
    public void indexProduct(Map<String, Object> productDocument) {
        if (!properties.isEnabled() || productDocument == null || productDocument.isEmpty()) {
            return;
        }
        try {
            post(indexPath() + "/documents?primaryKey=productId", List.of(normalizeDocument(productDocument)));
        } catch (Exception e) {
            log.warn("Failed to index product document productId={}", productDocument.get("productId"), e);
        }
    }

    @Override
    public void deleteProduct(Long productId) {
        if (!properties.isEnabled() || productId == null) {
            return;
        }
        try {
            restClient.delete()
                    .uri(indexPath() + "/documents/{productId}", productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to delete product document productId={}", productId, e);
        }
    }

    @Override
    public Map<String, Object> reindexProducts(List<Map<String, Object>> productDocuments) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("index", properties.getProductIndex());
        int documentCount = productDocuments == null ? 0 : productDocuments.size();
        result.put("documentCount", documentCount);
        if (!properties.isEnabled()) {
            result.put("status", "disabled");
            return result;
        }
        try {
            put(indexPath() + "/settings/filterable-attributes",
                    List.of("categoryId", "productType", "status", "shopId"));
            put(indexPath() + "/settings/sortable-attributes",
                    List.of("salePrice", "favoriteCount", "viewCount", "createdAt", "productId"));
            restClient.delete()
                    .uri(indexPath() + "/documents")
                    .retrieve()
                    .toBodilessEntity();
            List<Map<String, Object>> documents = productDocuments == null ? List.of() : productDocuments.stream()
                    .map(this::normalizeDocument)
                    .toList();
            if (!documents.isEmpty()) {
                Map<String, Object> task = post(indexPath() + "/documents?primaryKey=productId", documents);
                result.put("task", task);
            }
            result.put("status", "submitted");
            return result;
        } catch (Exception e) {
            log.warn("Failed to reindex product search documents", e);
            result.put("status", "failed");
            result.put("message", e.getMessage());
            return result;
        }
    }

    private List<String> filters(ProductSearchCriteria criteria) {
        List<String> filters = new ArrayList<>();
        filters.add("status = \"on_sale\"");
        if (criteria.categoryId() != null) {
            filters.add("categoryId = " + criteria.categoryId());
        }
        if (criteria.productType() != null && !criteria.productType().isBlank()) {
            filters.add("productType = \"" + escape(criteria.productType().trim()) + "\"");
        }
        return filters;
    }

    private List<Map<String, Object>> hits(Map<String, Object> response) {
        Object hits = response.get("hits");
        if (!(hits instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(hit -> objectMapper.convertValue(hit, new TypeReference<Map<String, Object>>() {
                }))
                .toList();
    }

    private Map<String, Object> normalizeDocument(Map<String, Object> document) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (String key : List.of("productId", "title", "subtitle", "description", "categoryId", "categoryName",
                "productType", "status", "salePrice", "favoriteCount", "viewCount", "createdAt", "shopId",
                "shopName")) {
            normalized.put(key, normalizeValue(document.get(key)));
        }
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().toEpochMilli();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof Instant instant) {
            return instant.toEpochMilli();
        }
        return value;
    }

    private Map<String, Object> post(String path, Object payload) {
        Object response = restClient.post()
                .uri(path)
                .body(payload)
                .retrieve()
                .body(Object.class);
        return objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {
        });
    }

    private void put(String path, Object payload) {
        restClient.put()
                .uri(path)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private String indexPath() {
        return "/indexes/" + encodePath(properties.getProductIndex());
    }

    private String encodePath(String value) {
        return URI.create("http://localhost/" + value).getRawPath().substring(1);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Optional<Long> toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        return Optional.of(Long.parseLong(String.valueOf(value)));
    }

    private static String normalizedHost(String host) {
        String normalized = host == null || host.isBlank() ? "http://localhost:7700" : host.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
