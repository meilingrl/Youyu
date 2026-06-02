package com.youyu.backend.service.payment.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.service.payment.AlipaySandboxProperties;
import com.youyu.backend.service.payment.PaymentGatewayService;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlipaySandboxGatewayServiceImplTest {

    private KeyPair keyPair;
    private AlipaySandboxProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        properties = new AlipaySandboxProperties();
        properties.setEnabled(true);
        properties.setAppId("sandbox-app");
        properties.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        properties.setAlipayPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        properties.setNotifyUrl("https://example.test/api/payments/callbacks/alipay-sandbox");
    }

    @Test
    void precreateReturnsQrCodeWithoutBrowserRedirect() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/gateway.do", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = signedResponse(
                    "alipay_trade_precreate_response",
                    "{\"code\":\"10000\",\"msg\":\"Success\",\"qr_code\":\"https://qr.example/pay\"}");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            properties.setGatewayUrl("http://localhost:" + server.getAddress().getPort() + "/gateway.do");
            Map<String, Object> payment = gateway().createPayment(
                    new PaymentGatewayService.PaymentInitiationRequest("PAY-1", "ORDER-1",
                            new BigDecimal("12.34"), "Demo order"));

            assertThat(payment).containsEntry("qrCode", "https://qr.example/pay");
            assertThat(payment).doesNotContainKey("redirectUrl");
            assertThat(requestBody.get()).contains("method=alipay.trade.precreate");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void precreateRejectsInvalidResponseSignature() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/gateway.do", exchange -> {
            byte[] response = """
                    {"alipay_trade_precreate_response":{"code":"10000","msg":"Success","qr_code":"https://qr.example/pay"},"sign":"invalid"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            properties.setGatewayUrl("http://localhost:" + server.getAddress().getPort() + "/gateway.do");

            assertThatThrownBy(() -> gateway().createPayment(
                    new PaymentGatewayService.PaymentInitiationRequest("PAY-1", "ORDER-1",
                            new BigDecimal("12.34"), "Demo order")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("signature verification failed");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void refundAcceptsVerifiedResponseSignature() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/gateway.do", exchange -> {
            byte[] response = signedResponse(
                    "alipay_trade_refund_response",
                    "{\"code\":\"10000\",\"msg\":\"Success\",\"trade_no\":\"ALI-1\"}");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            properties.setGatewayUrl("http://localhost:" + server.getAddress().getPort() + "/gateway.do");

            assertThat(gateway().refund(
                    new PaymentGatewayService.RefundRequest("PAY-1", "REF-1",
                            new BigDecimal("12.34"), "Demo refund")))
                    .containsEntry("status", "success")
                    .containsEntry("providerTradeNo", "ALI-1");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void verifiedCallbackMapsProviderStatuses() throws Exception {
        assertThat(gateway().verifyCallback(signedCallback("TRADE_SUCCESS")).paymentStatus()).isEqualTo("success");
        assertThat(gateway().verifyCallback(signedCallback("TRADE_CLOSED")).paymentStatus()).isEqualTo("cancelled");
        assertThat(gateway().verifyCallback(signedCallback("WAIT_BUYER_PAY")).paymentStatus()).isEqualTo("failed");
    }

    @Test
    void callbackRejectsInvalidSignature() throws Exception {
        Map<String, String> callback = signedCallback("TRADE_SUCCESS");
        callback.put("total_amount", "99.99");

        assertThatThrownBy(() -> gateway().verifyCallback(callback))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void enabledAdapterRejectsMissingConfiguration() {
        properties.setPrivateKey("");

        assertThat(gateway().available()).isFalse();
        assertThatThrownBy(() -> gateway().createPayment(
                new PaymentGatewayService.PaymentInitiationRequest("PAY-1", "ORDER-1",
                        new BigDecimal("12.34"), "Demo order")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not fully configured");
    }

    private AlipaySandboxGatewayServiceImpl gateway() {
        return new AlipaySandboxGatewayServiceImpl(properties, new ObjectMapper());
    }

    private Map<String, String> signedCallback(String tradeStatus) throws Exception {
        Map<String, String> callback = new LinkedHashMap<>();
        callback.put("app_id", properties.getAppId());
        callback.put("out_trade_no", "PAY-1");
        callback.put("trade_no", "ALI-1");
        callback.put("trade_status", tradeStatus);
        callback.put("total_amount", "12.34");
        callback.put("sign_type", "RSA2");
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(canonical(callback).getBytes(StandardCharsets.UTF_8));
        callback.put("sign", Base64.getEncoder().encodeToString(signature.sign()));
        return callback;
    }

    private byte[] signedResponse(String responseKey, String responseContent) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(keyPair.getPrivate());
            signature.update(responseContent.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = Base64.getEncoder().encodeToString(signature.sign());
            return ("{\"" + responseKey + "\":" + responseContent + ",\"sign\":\"" + encodedSignature + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String canonical(Map<String, String> parameters) {
        return new TreeMap<>(parameters).entrySet().stream()
                .filter(entry -> !"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }
}
