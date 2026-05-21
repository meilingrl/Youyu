package com.campusmarket.backend.service.order;

import java.util.List;
import java.util.Map;

public interface OrderService {

    Map<String, Object> cart(Long userId);

    Map<String, Object> addCartItem(Long userId, Map<String, Object> command);

    Map<String, Object> updateCartItem(Long userId, Long cartItemId, Map<String, Object> command);

    void removeCartItem(Long userId, Long cartItemId);

    Map<String, Object> previewOrder(Long userId, Map<String, Object> command);

    Map<String, Object> createOrder(Long userId, Map<String, Object> command);

    List<Map<String, Object>> listOrders(Long userId);

    List<Map<String, Object>> listAdminOrders();

    Map<String, Object> getOrderDetail(Long userId, Long orderId, boolean adminView);

    Map<String, Object> cancelOrder(Long userId, Long orderId);

    Map<String, Object> confirmReceipt(Long userId, Long orderId);

    Map<String, Object> sellerShip(Long orderId, Map<String, Object> command);

    Map<String, Object> sellerConfirmOffline(Long orderId);

    Map<String, Object> buyerConfirmOffline(Long userId, Long orderId);

    Map<String, Object> applyRefund(Long userId, Long orderId, Map<String, Object> command);

    Map<String, Object> completeRefund(Long orderId, Long refundId);

    Map<String, Object> accessDigitalAsset(Long userId, Long orderId, Long assetId);
}
