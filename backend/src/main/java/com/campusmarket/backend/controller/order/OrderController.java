package com.campusmarket.backend.controller.order;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.auth.AuthContextHolder;
import com.campusmarket.backend.common.auth.LoginRequired;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@LoginRequired
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listOrders(HttpServletRequest request) {
        return ApiResponse.success(orderService.listOrders(currentUserId()), traceId(request));
    }

    @PostMapping("/preview")
    public ApiResponse<Map<String, Object>> preview(@RequestBody Map<String, Object> command,
                                                    HttpServletRequest request) {
        return ApiResponse.success(orderService.previewOrder(currentUserId(), command), traceId(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> command,
                                                        HttpServletRequest request) {
        return ApiResponse.success(orderService.createOrder(currentUserId(), command), traceId(request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long orderId, HttpServletRequest request) {
        return ApiResponse.success(orderService.getOrderDetail(currentUserId(), orderId, false), traceId(request));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@PathVariable Long orderId, HttpServletRequest request) {
        return ApiResponse.success(orderService.cancelOrder(currentUserId(), orderId), traceId(request));
    }

    @PostMapping("/{orderId}/confirm-receipt")
    public ApiResponse<Map<String, Object>> confirmReceipt(@PathVariable Long orderId, HttpServletRequest request) {
        return ApiResponse.success(orderService.confirmReceipt(currentUserId(), orderId), traceId(request));
    }

    @PostMapping("/{orderId}/offline/buyer-confirm")
    public ApiResponse<Map<String, Object>> buyerConfirmOffline(@PathVariable Long orderId, HttpServletRequest request) {
        return ApiResponse.success(orderService.buyerConfirmOffline(currentUserId(), orderId), traceId(request));
    }

    @PostMapping("/{orderId}/refunds")
    public ApiResponse<Map<String, Object>> applyRefund(@PathVariable Long orderId,
                                                        @RequestBody Map<String, Object> command,
                                                        HttpServletRequest request) {
        return ApiResponse.success(orderService.applyRefund(currentUserId(), orderId, command), traceId(request));
    }

    @GetMapping("/{orderId}/assets/{assetId}/access")
    public ApiResponse<Map<String, Object>> accessDigitalAsset(@PathVariable Long orderId,
                                                                @PathVariable Long assetId,
                                                                HttpServletRequest request) {
        return ApiResponse.success(orderService.accessDigitalAsset(currentUserId(), orderId, assetId), traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
