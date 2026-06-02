package com.youyu.backend.service.product.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.auth.AdminPermissionPolicy;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.product.ProductReviewTaskMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.product.ProductService;
import com.youyu.backend.service.search.SearchService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String DEFAULT_COVER =
            "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=900&q=80";

    private final ProductMapper productMapper;
    private final ProductReviewTaskMapper productReviewTaskMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final SearchService searchService;

    public ProductServiceImpl(ProductMapper productMapper,
                              ProductReviewTaskMapper productReviewTaskMapper,
                              ShopMapper shopMapper,
                              UserMapper userMapper,
                              SearchService searchService) {
        this.productMapper = productMapper;
        this.productReviewTaskMapper = productReviewTaskMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.searchService = searchService;
    }

    @Override
    public Map<String, Object> listProducts(String keyword, Long categoryId, String productType, Long viewerUserId, int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.min(50, Math.max(1, pageSize));
        int offset = (page - 1) * pageSize;

        List<Map<String, Object>> items = productMapper.findPublicByFiltersPaged(keyword, categoryId, productType, offset, pageSize)
                .stream()
                .map(this::listItem)
                .toList();
        long total = productMapper.countPublicByFilters(keyword, categoryId, productType);

        searchService.recordKeywordSearch(keyword, viewerUserId, items.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    @Override
    public Map<String, Object> getProductDetail(Long productId) {
        Map<String, Object> product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Product not found"));
        if (!isVisibleToCurrentUser(product)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Product not found");
        }
        Map<String, Object> detail = new LinkedHashMap<>(product);
        List<Map<String, Object>> media = productMapper.findMediaByProductId(productId);
        detail.put("media", media.stream().map(item -> item.get("url")).toList());
        detail.put("previewAssets", productMapper.findDigitalAssetsByProductId(productId).stream()
                .filter(item -> Boolean.TRUE.equals(item.get("isPreviewAsset")))
                .toList());
        detail.put("allowedFulfillmentTypes", resolveAllowedFulfillmentTypes(product));
        detail.put("fullAssetLocked", "digital".equals(product.get("productType")));
        return detail;
    }

    @Override
    public List<Map<String, Object>> listMyProducts(Long sellerUserId) {
        return productMapper.findBySellerId(sellerUserId);
    }

    @Override
    public List<Map<String, Object>> listFavorites(Long userId) {
        return productMapper.findFavoritesByUserId(userId)
                .stream()
                .map(this::listItem)
                .toList();
    }

    @Override
    @Transactional
    public Map<String, Object> addFavorite(Long userId, Long productId) {
        Map<String, Object> product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Product not found"));
        if (!isFavoriteableProduct(product)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Product not found");
        }

        boolean existed = productMapper.isFavorite(userId, productId);
        productMapper.addFavorite(userId, productId);
        if (!existed) {
            productMapper.updateFavoriteCount(productId, 1);
        }
        return favoriteResult(productId, true);
    }

    @Override
    @Transactional
    public Map<String, Object> removeFavorite(Long userId, Long productId) {
        boolean removed = productMapper.removeFavorite(userId, productId);
        if (removed) {
            productMapper.updateFavoriteCount(productId, -1);
        }
        return favoriteResult(productId, false);
    }

    @Override
    public Map<String, Object> publishProduct(Long sellerUserId, Map<String, Object> command) {
        assertCanPublish(sellerUserId);
        Map<String, Object> product = normalizeProduct(sellerUserId, command);
        Long productId = productMapper.insert(product);
        productMapper.replaceMedia(productId, mediaUrls(command, product));
        if ("pending_review".equals(product.get("reviewStatus"))) {
            productReviewTaskMapper.insertPending(productId);
        }
        return productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Product not found"));
    }

    @Override
    public Map<String, Object> updateProduct(Long sellerUserId, Long productId, Map<String, Object> command) {
        Map<String, Object> existing = findOwnedProduct(sellerUserId, productId);
        if ("on_sale".equals(existing.get("status"))) {
            productMapper.updateStatus(productId, "off_sale");
        }
        Map<String, Object> product = normalizeProduct(sellerUserId, command);
        productMapper.update(productId, product);
        productMapper.replaceMedia(productId, mediaUrls(command, product));
        if ("pending_review".equals(product.get("reviewStatus"))) {
            productReviewTaskMapper.insertPending(productId);
        }
        return productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Product not found"));
    }

    @Override
    public Map<String, Object> updateProductStatus(Long sellerUserId, Long productId, String status) {
        Map<String, Object> product = findOwnedProduct(sellerUserId, productId);
        if (!List.of("off_sale", "closed").contains(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Only off_sale or closed is supported by seller");
        }
        productMapper.updateStatus(productId, status);
        Map<String, Object> updated = new LinkedHashMap<>(product);
        updated.put("status", status);
        return updated;
    }

    @Override
    public Map<String, Object> deleteProduct(Long sellerUserId, Long productId) {
        Map<String, Object> product = findOwnedProduct(sellerUserId, productId);
        productMapper.softDelete(productId);
        product.put("status", "closed");
        return product;
    }

    private Map<String, Object> normalizeProduct(Long sellerUserId, Map<String, Object> command) {
        String title = requiredString(command, "title");
        Long categoryId = toLong(first(command, "categoryId"));
        if (categoryId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "categoryId is required");
        }
        String productType = defaultString(first(command, "productType", "type"), "physical");
        boolean submitted = !"draft".equals(defaultString(first(command, "submitMode"), "submit"));
        boolean digital = "digital".equals(productType);
        List<String> deliveryMethods = deliveryMethods(command);
        if (deliveryMethods.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "At least one delivery method is required");
        }

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("sellerUserId", sellerUserId);
        product.put("shopId", approvedShopId(sellerUserId));
        product.put("categoryId", categoryId);
        product.put("title", title);
        product.put("subtitle", defaultString(first(command, "subtitle"), ""));
        product.put("description", defaultString(first(command, "description"), ""));
        product.put("productType", productType);
        product.put("status", submitted && !digital ? "on_sale" : submitted ? "off_sale" : "draft");
        product.put("reviewStatus", digital && submitted ? "pending_review" : "not_required");
        product.put("coverUrl", defaultString(first(command, "coverUrl", "cover"), DEFAULT_COVER));
        product.put("salePrice", decimal(first(command, "salePrice", "price")));
        product.put("originalPrice", first(command, "originalPrice"));
        product.put("stock", Math.max(1, intValue(first(command, "stock", "stockQuantity"), 1)));
        product.put("supportsLogistics", deliveryMethods.contains("logistics"));
        product.put("supportsOfflineDelivery", deliveryMethods.contains("offline"));
        product.put("supportsDigitalDelivery", deliveryMethods.contains("digital"));
        product.put("allowPreview", Boolean.TRUE.equals(first(command, "allowPreview")));
        product.put("previewRuleText", defaultString(first(command, "previewRuleText", "previewHint"), ""));
        return product;
    }

    private boolean isVisibleToCurrentUser(Map<String, Object> product) {
        boolean publicProduct = isFavoriteableProduct(product);
        if (publicProduct) {
            return true;
        }
        if (AuthContextHolder.get() == null) {
            return false;
        }
        return AdminPermissionPolicy.isAdminRole(AuthContextHolder.get().getRole())
                || Objects.equals(AuthContextHolder.get().getUserId(), product.get("sellerUserId"));
    }

    private boolean isFavoriteableProduct(Map<String, Object> product) {
        return "on_sale".equals(product.get("status"))
                && ("not_required".equals(product.get("reviewStatus")) || "approved".equals(product.get("reviewStatus")));
    }

    private Map<String, Object> favoriteResult(Long productId, boolean favorite) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", productId);
        result.put("favorite", favorite);
        return result;
    }

    private void assertCanPublish(Long sellerUserId) {
        Map<String, Object> user = userMapper.findById(sellerUserId)
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN, "User is not available"));
        boolean canPublish = Boolean.TRUE.equals(user.get("canPublish"));
        boolean restricted = Boolean.TRUE.equals(user.get("isRestricted"));
        if (!canPublish || restricted) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only verified users can publish products");
        }
    }

    private Long approvedShopId(Long sellerUserId) {
        return shopMapper.findByOwnerUserId(sellerUserId)
                .filter(shop -> "active".equals(shop.get("status")) && "approved".equals(shop.get("reviewStatus")))
                .map(shop -> toLong(shop.get("id")))
                .orElse(null);
    }

    private Map<String, Object> findOwnedProduct(Long sellerUserId, Long productId) {
        Map<String, Object> product = productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Product not found"));
        if (!Objects.equals(sellerUserId, product.get("sellerUserId"))) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Product does not belong to current user");
        }
        return product;
    }

    private Map<String, Object> listItem(Map<String, Object> product) {
        Map<String, Object> item = new LinkedHashMap<>();
        for (String key : List.of("id", "title", "categoryName", "productType", "type", "status",
                "reviewStatus", "salePrice", "price", "coverUrl", "cover", "shopId", "shopName",
                "sellerName", "supportsLogistics", "supportsOfflineDelivery", "supportsDigitalDelivery",
                "publishedAt", "favoriteCount", "viewCount")) {
            item.put(key, product.get(key));
        }
        item.put("deliveryMethods", resolveAllowedFulfillmentTypes(product));
        return item;
    }

    private List<String> resolveAllowedFulfillmentTypes(Map<String, Object> product) {
        List<String> allowed = new ArrayList<>();
        if (Boolean.TRUE.equals(product.get("supportsLogistics"))) {
            allowed.add("logistics");
        }
        if (Boolean.TRUE.equals(product.get("supportsOfflineDelivery"))) {
            allowed.add("offline");
        }
        if (Boolean.TRUE.equals(product.get("supportsDigitalDelivery"))) {
            allowed.add("digital");
        }
        return allowed;
    }

    @SuppressWarnings("unchecked")
    private List<String> mediaUrls(Map<String, Object> command, Map<String, Object> product) {
        Object media = first(command, "media", "mediaUrls");
        if (media instanceof List<?> list) {
            List<String> urls = list.stream().filter(Objects::nonNull).map(String::valueOf).filter(s -> !s.isBlank()).toList();
            if (!urls.isEmpty()) {
                return urls;
            }
        }
        return List.of(String.valueOf(product.get("coverUrl")));
    }

    private List<String> deliveryMethods(Map<String, Object> command) {
        Object raw = first(command, "deliveryMethods", "fulfillmentTypes");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .map(value -> switch (value) {
                    case "offline_face_to_face" -> "offline";
                    case "digital_delivery" -> "digital";
                    default -> value;
                })
                .distinct()
                .toList();
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

    private BigDecimal decimal(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "price is required");
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Long toLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int intValue(Object value, int fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
