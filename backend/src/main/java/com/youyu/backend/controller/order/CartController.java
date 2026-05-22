package com.youyu.backend.controller.order;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@LoginRequired
public class CartController {

    private final OrderService orderService;

    public CartController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> cart(HttpServletRequest request) {
        return ApiResponse.success(orderService.cart(currentUserId()), traceId(request));
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> addCartItem(@RequestBody Map<String, Object> command,
                                                        HttpServletRequest request) {
        return ApiResponse.success(orderService.addCartItem(currentUserId(), command), traceId(request));
    }

    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<Map<String, Object>> updateCartItem(@PathVariable Long cartItemId,
                                                           @RequestBody Map<String, Object> command,
                                                           HttpServletRequest request) {
        return ApiResponse.success(orderService.updateCartItem(currentUserId(), cartItemId, command), traceId(request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Map<String, Object>> removeCartItem(@PathVariable Long cartItemId,
                                                           HttpServletRequest request) {
        orderService.removeCartItem(currentUserId(), cartItemId);
        return ApiResponse.success(Map.of("removed", true), traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
