package com.youyu.backend.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "youyu.logistics")
public class LogisticsIntegrationProperties {

    private final Amap amap = new Amap();
    private final Tracking tracking = new Tracking();

    public Amap getAmap() {
        return amap;
    }

    public Tracking getTracking() {
        return tracking;
    }

    public static class Amap {
        private boolean enabled;
        private String webServiceKey = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWebServiceKey() {
            return webServiceKey;
        }

        public void setWebServiceKey(String webServiceKey) {
            this.webServiceKey = defaultString(webServiceKey);
        }

        public boolean isConfigured() {
            return enabled && !webServiceKey.isBlank();
        }
    }

    public static class Tracking {
        private String provider = "disabled";
        private boolean enabled;
        private String kdniaoBusinessId = "";
        private String kdniaoAppKey = "";
        private String kdniaoEndpoint = "https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx";
        private int timeoutSeconds = 5;
        private Map<String, String> carrierCodes = defaultCarrierCodes();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = defaultString(provider).isBlank() ? "disabled" : provider.trim();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKdniaoBusinessId() {
            return kdniaoBusinessId;
        }

        public void setKdniaoBusinessId(String kdniaoBusinessId) {
            this.kdniaoBusinessId = defaultString(kdniaoBusinessId);
        }

        public String getKdniaoAppKey() {
            return kdniaoAppKey;
        }

        public void setKdniaoAppKey(String kdniaoAppKey) {
            this.kdniaoAppKey = defaultString(kdniaoAppKey);
        }

        public String getKdniaoEndpoint() {
            return kdniaoEndpoint;
        }

        public void setKdniaoEndpoint(String kdniaoEndpoint) {
            this.kdniaoEndpoint = defaultString(kdniaoEndpoint).isBlank()
                    ? "https://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx"
                    : kdniaoEndpoint.trim();
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = Math.max(1, timeoutSeconds);
        }

        public Map<String, String> getCarrierCodes() {
            return carrierCodes;
        }

        public void setCarrierCodes(Map<String, String> carrierCodes) {
            this.carrierCodes = carrierCodes == null ? defaultCarrierCodes() : carrierCodes;
        }

        public boolean isKdniaoConfigured() {
            return enabled
                    && "kdniao".equalsIgnoreCase(provider)
                    && !kdniaoBusinessId.isBlank()
                    && !kdniaoAppKey.isBlank()
                    && !kdniaoEndpoint.isBlank();
        }

        public String resolveCarrierCode(String company) {
            if (company == null || company.isBlank()) {
                return "";
            }
            String normalized = company.trim();
            return carrierCodes.getOrDefault(normalized, normalized);
        }

        private static Map<String, String> defaultCarrierCodes() {
            Map<String, String> defaults = new LinkedHashMap<>();
            defaults.put("SF Express", "SF");
            defaults.put("顺丰速运", "SF");
            defaults.put("YTO Express", "YTO");
            defaults.put("圆通速递", "YTO");
            defaults.put("ZTO Express", "ZTO");
            defaults.put("中通快递", "ZTO");
            defaults.put("STO Express", "STO");
            defaults.put("申通快递", "STO");
            defaults.put("Yunda Express", "YD");
            defaults.put("韵达快递", "YD");
            defaults.put("EMS", "EMS");
            return defaults;
        }
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }
}
