package com.youyu.backend.service.shop.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.shop.ShopService;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public ShopServiceImpl(ShopMapper shopMapper, ProductMapper productMapper, UserMapper userMapper) {
        this.shopMapper = shopMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> moduleInfo() {
        return Map.of(
                "module", "shop",
                "status", "persistent",
                "next", "Shop application, review and storefront are backed by database"
        );
    }

    @Override
    public Map<String, Object> insightSnapshot(Long shopId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startInclusive = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<String, Object> summary = shopMapper.summarizeMonthlyInsight(shopId, startInclusive, endExclusive);
        return linkedMap(
                "shopId", shopId,
                "monthlySalesAmount", summary.get("monthlySalesAmount"),
                "monthlyOrderCount", summary.get("monthlyOrderCount"),
                "hotProducts", shopMapper.findHotProducts(shopId, startInclusive, endExclusive, 5),
                "viewCountSummary", summary.get("viewCountSummary"),
                "favoriteCountSummary", summary.get("favoriteCountSummary"),
                "repeatBuyerCount", summary.get("repeatBuyerCount"),
                "lastCalculatedAt", DATETIME_FORMATTER.format(LocalDateTime.now()),
                "metricSource", "real_query"
        );
    }

    @Override
    public Map<String, Object> getShopDetail(Long shopId) {
        Map<String, Object> shop = shopMapper.findById(shopId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Shop not found"));
        if (!"active".equals(shop.get("status")) || !"approved".equals(shop.get("reviewStatus"))) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Shop not found");
        }
        return composeShopDetail(shop, true);
    }

    @Override
    public Map<String, Object> getMyShop(Long ownerUserId) {
        return shopMapper.findByOwnerUserId(ownerUserId)
                .map(shop -> composeShopDetail(shop, false))
                .orElseGet(() -> linkedMap("shop", Map.of(), "products", List.of(), "capability", Map.of()));
    }

    @Override
    public Map<String, Object> applyShop(Long ownerUserId, Map<String, Object> command) {
        assertCanApplyShop(ownerUserId);
        if (shopMapper.findByOwnerUserId(ownerUserId).isPresent()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Current user already has a shop or application");
        }
        Map<String, Object> shop = new LinkedHashMap<>();
        shop.put("ownerUserId", ownerUserId);
        shop.put("name", requiredString(command, "name"));
        shop.put("description", defaultString(command.get("description"), ""));
        shop.put("coverUrl", defaultString(first(command, "coverUrl", "cover"), ""));
        shop.put("announcement", defaultString(first(command, "announcement", "notice"), ""));
        try {
            Long shopId = shopMapper.insertApplication(shop);
            return composeShopDetail(shopMapper.findById(shopId)
                    .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Shop not found")), false);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Shop name is already used");
        }
    }

    private Map<String, Object> composeShopDetail(Map<String, Object> shop, boolean publicOnly) {
        Long shopId = toLong(shop.get("id"));
        List<Map<String, Object>> products = productMapper.findByShopId(shopId);
        if (publicOnly) {
            products = products.stream()
                    .filter(item -> "on_sale".equals(item.get("status")))
                    .filter(item -> "approved".equals(item.get("reviewStatus")) || "not_required".equals(item.get("reviewStatus")))
                    .toList();
        }
        return linkedMap(
                "shop", shop,
                "capability", shopMapper.findCapabilityByShopId(shopId).orElse(Map.of()),
                "products", products
        );
    }

    private void assertCanApplyShop(Long ownerUserId) {
        Map<String, Object> user = userMapper.findById(ownerUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN, "User is not available"));
        boolean canApplyShop = Boolean.TRUE.equals(user.get("canOpenShop"))
                || Boolean.TRUE.equals(user.get("canApplyShop"));
        boolean restricted = Boolean.TRUE.equals(user.get("isRestricted"));
        if (!canApplyShop || restricted) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only verified users can apply for a shop");
        }
    }

    private Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private String requiredString(Map<String, Object> map, String key) {
        String value = defaultString(map.get(key), "");
        if (value.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " is required");
        }
        return value;
    }

    private String defaultString(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
