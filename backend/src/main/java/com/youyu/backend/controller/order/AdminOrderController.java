package com.youyu.backend.controller.order;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AdminPermission;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.order.OrderService;
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
@RequestMapping("/api/admin/orders")
@LoginRequired(roles = {UserRole.ADMIN})
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @LoginRequired(permissions = {AdminPermission.ADMIN_ORDERS_READ})
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.success(orderService.listAdminOrders(), traceId(request));
    }

    @GetMapping("/{orderId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_ORDERS_READ})
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long orderId, HttpServletRequest request) {
        return ApiResponse.success(orderService.getOrderDetail(null, orderId, true), traceId(request));
    }

    @PostMapping("/{orderId}/ship")
    @LoginRequired(permissions = {AdminPermission.ADMIN_ORDERS_MANAGE})
    public ApiResponse<Map<String, Object>> ship(@PathVariable Long orderId,
                                                 @RequestBody Map<String, Object> command,
                                                 HttpServletRequest request) {
        return ApiResponse.success(orderService.sellerShip(orderId, command), traceId(request));
    }

    @PostMapping("/{orderId}/offline/seller-confirm")
    @LoginRequired(permissions = {AdminPermission.ADMIN_ORDERS_MANAGE})
    public ApiResponse<Map<String, Object>> sellerConfirmOffline(@PathVariable Long orderId,
                                                                 HttpServletRequest request) {
        return ApiResponse.success(orderService.sellerConfirmOffline(orderId), traceId(request));
    }

    @PostMapping("/{orderId}/refunds/{refundId}/complete")
    @LoginRequired(permissions = {AdminPermission.ADMIN_ORDERS_MANAGE})
    public ApiResponse<Map<String, Object>> completeRefund(@PathVariable Long orderId,
                                                           @PathVariable Long refundId,
                                                           HttpServletRequest request) {
        return ApiResponse.success(orderService.completeRefund(orderId, refundId), traceId(request));
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
