package com.youyu.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "youyu.cache.redis")
public class RedisCacheProperties {

    private boolean enabled = false;
    private String keyPrefix = "youyu:cache:";
    private Duration hotSearchTtl = Duration.ofHours(1);
    private Duration homeRecommendTtl = Duration.ofMinutes(15);
    private Duration alsoBoughtTtl = Duration.ofMinutes(15);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Duration getHotSearchTtl() {
        return hotSearchTtl;
    }

    public void setHotSearchTtl(Duration hotSearchTtl) {
        this.hotSearchTtl = hotSearchTtl;
    }

    public Duration getHomeRecommendTtl() {
        return homeRecommendTtl;
    }

    public void setHomeRecommendTtl(Duration homeRecommendTtl) {
        this.homeRecommendTtl = homeRecommendTtl;
    }

    public Duration getAlsoBoughtTtl() {
        return alsoBoughtTtl;
    }

    public void setAlsoBoughtTtl(Duration alsoBoughtTtl) {
        this.alsoBoughtTtl = alsoBoughtTtl;
    }
}
