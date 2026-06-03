package com.youyu.backend.controller.payment;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@LoginRequired
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/gateway")
    public ApiResponse<Map<String, Object>> gateway(HttpServletRequest request) {
        return ApiResponse.success(paymentService.gatewayInfo(), traceId(request));
    }

    @PostMapping("/orders/{orderId}/initiate")
    public ApiResponse<Map<String, Object>> initiate(@PathVariable Long orderId,
                                                     @RequestParam(defaultValue = "mock") String paymentMethod,
                                                     HttpServletRequest request) {
        return ApiResponse.success(paymentService.initiatePayment(currentUserId(), orderId, paymentMethod), traceId(request));
    }

    @PostMapping("/{paymentNo}/resume")
    public ApiResponse<Map<String, Object>> resume(@PathVariable String paymentNo, HttpServletRequest request) {
        return ApiResponse.success(paymentService.resumePayment(currentUserId(), paymentNo), traceId(request));
    }

    @PostMapping("/{paymentNo}/mock-success")
    public ApiResponse<Map<String, Object>> mockSuccess(@PathVariable String paymentNo, HttpServletRequest request) {
        return ApiResponse.success(paymentService.completeMockPayment(currentUserId(), paymentNo), traceId(request));
    }

    private Long currentUserId() {
        return AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
