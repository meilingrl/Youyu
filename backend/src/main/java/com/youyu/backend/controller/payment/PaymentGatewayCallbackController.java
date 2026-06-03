package com.youyu.backend.controller.payment;

import com.youyu.backend.service.payment.PaymentService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/callbacks")
public class PaymentGatewayCallbackController {

    private final PaymentService paymentService;

    public PaymentGatewayCallbackController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/alipay-sandbox")
    public String alipaySandbox(@RequestParam Map<String, String> parameters) {
        paymentService.processGatewayCallback("alipay_sandbox", parameters);
        return "success";
    }
}
