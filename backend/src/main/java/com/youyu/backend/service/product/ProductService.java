package com.youyu.backend.service.product;

import java.util.List;
import java.util.Map;

public interface ProductService {

    Map<String, Object> listProducts(String keyword, Long categoryId, String productType, Long viewerUserId, int page, int pageSize);

    Map<String, Object> getProductDetail(Long productId);

    List<Map<String, Object>> listMyProducts(Long sellerUserId);

    Map<String, Object> publishProduct(Long sellerUserId, Map<String, Object> command);

    Map<String, Object> updateProduct(Long sellerUserId, Long productId, Map<String, Object> command);

    Map<String, Object> updateProductStatus(Long sellerUserId, Long productId, String status);

    Map<String, Object> deleteProduct(Long sellerUserId, Long productId);
}
