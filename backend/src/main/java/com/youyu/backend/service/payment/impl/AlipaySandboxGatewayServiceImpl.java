package com.youyu.backend.service.payment.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.service.payment.AlipaySandboxProperties;
import com.youyu.backend.service.payment.PaymentGatewayService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlipaySandboxGatewayServiceImpl implements PaymentGatewayService {

    private static final String CHARSET = "UTF-8";
    private final AlipaySandboxProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public AlipaySandboxGatewayServiceImpl(AlipaySandboxProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newHttpClient());
    }

    AlipaySandboxGatewayServiceImpl(AlipaySandboxProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String paymentMethod() {
        return "alipay_sandbox";
    }

    @Override
    public String gatewayCode() {
        return "alipay_sandbox";
    }

    @Override
    public boolean available() {
        return properties.isConfigured();
    }

    @Override
    public Map<String, Object> createPayment(PaymentInitiationRequest request) {
        validateConfiguration();
        Map<String, String> parameters = commonParameters("alipay.trade.precreate");
        parameters.put("notify_url", properties.getNotifyUrl());
        parameters.put("biz_content", json(Map.of(
                "out_trade_no", request.paymentNo(),
                "total_amount", request.amount().toPlainString(),
                "subject", request.subject()
        )));
        parameters.put("sign", sign(parameters));
        Map<String, Object> response = execute(parameters, "alipay_trade_precreate_response");
        return Map.of(
                "gateway", gatewayCode(),
                "paymentMethod", paymentMethod(),
                "paymentNo", request.paymentNo(),
                "status", "pending",
                "qrCode", requiredResponse(response, "qr_code")
        );
    }

    @Override
    public GatewayCallbackResult verifyCallback(Map<String, String> parameters) {
        validateConfiguration();
        if (!properties.getAppId().equals(parameters.get("app_id"))) {
            throw invalidCallback("Alipay callback app_id mismatch");
        }
        String signature = parameters.get("sign");
        if (signature == null || !verify(parameters, signature)) {
            throw invalidCallback("Alipay callback signature verification failed");
        }
        String paymentNo = required(parameters, "out_trade_no");
        BigDecimal amount;
        try {
            amount = new BigDecimal(required(parameters, "total_amount"));
        } catch (NumberFormatException ex) {
            throw invalidCallback("Alipay callback amount is invalid");
        }
        String tradeStatus = required(parameters, "trade_status");
        String paymentStatus = switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" -> "success";
            case "TRADE_CLOSED" -> "cancelled";
            default -> "failed";
        };
        return new GatewayCallbackResult(
                paymentNo,
                parameters.getOrDefault("trade_no", ""),
                paymentStatus,
                amount,
                digest(canonical(parameters, false)),
                "trade_status=" + tradeStatus
        );
    }

    @Override
    public Map<String, Object> refund(RefundRequest request) {
        validateConfiguration();
        Map<String, String> parameters = commonParameters("alipay.trade.refund");
        parameters.put("biz_content", json(Map.of(
                "out_trade_no", request.paymentNo(),
                "out_request_no", request.refundNo(),
                "refund_amount", request.amount().toPlainString(),
                "refund_reason", request.reason()
        )));
        parameters.put("sign", sign(parameters));
        Map<String, Object> response = execute(parameters, "alipay_trade_refund_response");
        return Map.of(
                "gateway", gatewayCode(),
                "refundNo", request.refundNo(),
                "status", "success",
                "providerTradeNo", String.valueOf(response.getOrDefault("trade_no", ""))
        );
    }

    private Map<String, String> commonParameters(String method) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("app_id", properties.getAppId());
        parameters.put("method", method);
        parameters.put("format", "JSON");
        parameters.put("charset", CHARSET);
        parameters.put("sign_type", "RSA2");
        parameters.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        parameters.put("version", "1.0");
        return parameters;
    }

    private void validateConfiguration() {
        if (!properties.isConfigured()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Alipay sandbox is enabled but not fully configured");
        }
    }

    private String sign(Map<String, String> parameters) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey(properties.getPrivateKey()));
            signature.update(canonical(parameters, false).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Unable to sign Alipay sandbox request");
        }
    }

    private boolean verify(Map<String, String> parameters, String encodedSignature) {
        return verify(canonical(parameters, true), encodedSignature);
    }

    private boolean verify(String content, String encodedSignature) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey(properties.getAlipayPublicKey()));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(encodedSignature));
        } catch (Exception ex) {
            return false;
        }
    }

    private PrivateKey privateKey(String key) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodeKey(key)));
    }

    private PublicKey publicKey(String key) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodeKey(key)));
    }

    private byte[] decodeKey(String key) {
        String normalized = key.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private String canonical(Map<String, String> parameters, boolean excludeSignature) {
        return new TreeMap<>(parameters).entrySet().stream()
                .filter(entry -> !entry.getValue().isBlank())
                .filter(entry -> !excludeSignature || (!"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey())))
                .filter(entry -> excludeSignature || !"sign".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String encoded(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> execute(Map<String, String> parameters, String responseKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getGatewayUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET)
                    .POST(HttpRequest.BodyPublishers.ofString(encoded(parameters)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body();
            Map<String, Object> payload = objectMapper.readValue(responseBody, Map.class);
            Object gatewayResponse = payload.get(responseKey);
            if (!(gatewayResponse instanceof Map<?, ?> responseMap)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "Alipay sandbox response is invalid");
            }
            Object responseSignature = payload.get("sign");
            if (responseSignature == null
                    || !verify(responseContent(responseBody, responseKey), String.valueOf(responseSignature))) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "Alipay sandbox response signature verification failed");
            }
            Map<String, Object> normalized = (Map<String, Object>) responseMap;
            if (!"10000".equals(String.valueOf(normalized.get("code")))) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR,
                        "Alipay sandbox request failed: " + normalized.getOrDefault("sub_msg", normalized.get("msg")));
            }
            return normalized;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Unable to reach Alipay sandbox");
        }
    }

    private String responseContent(String responseBody, String responseKey) {
        String marker = "\"" + responseKey + "\":";
        int markerIndex = responseBody.indexOf(marker);
        if (markerIndex < 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Alipay sandbox response is invalid");
        }

        int objectStart = responseBody.indexOf('{', markerIndex + marker.length());
        if (objectStart < 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Alipay sandbox response is invalid");
        }

        boolean inString = false;
        boolean escaping = false;
        int depth = 0;
        for (int i = objectStart; i < responseBody.length(); i++) {
            char current = responseBody.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }
            if (current == '\\') {
                escaping = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return responseBody.substring(objectStart, i + 1);
                }
            }
        }

        throw new BusinessException(ResultCode.BUSINESS_ERROR, "Alipay sandbox response is invalid");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String json(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Unable to create Alipay sandbox request");
        }
    }

    private String required(Map<String, String> parameters, String key) {
        String value = parameters.get(key);
        if (value == null || value.isBlank()) {
            throw invalidCallback("Alipay callback is missing " + key);
        }
        return value;
    }

    private String requiredResponse(Map<String, Object> response, String key) {
        Object value = response.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Alipay sandbox response is missing " + key);
        }
        return String.valueOf(value);
    }

    private String digest(String value) {
        try {
            return Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private BusinessException invalidCallback(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
