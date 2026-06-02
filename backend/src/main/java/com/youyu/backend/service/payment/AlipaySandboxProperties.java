package com.youyu.backend.service.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "youyu.payment.alipay-sandbox")
public class AlipaySandboxProperties {

    private boolean enabled;
    private String appId = "";
    private String privateKey = "";
    private String alipayPublicKey = "";
    private String notifyUrl = "";
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    public boolean isConfigured() {
        return enabled
                && !appId.isBlank()
                && !privateKey.isBlank()
                && !alipayPublicKey.isBlank()
                && !notifyUrl.isBlank()
                && !gatewayUrl.isBlank();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getAlipayPublicKey() { return alipayPublicKey; }
    public void setAlipayPublicKey(String alipayPublicKey) { this.alipayPublicKey = alipayPublicKey; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }
}
