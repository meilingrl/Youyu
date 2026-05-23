package com.youyu.backend.entity.shop;

import com.youyu.backend.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ShopInsightSnapshot extends BaseEntity {

    private Long shopId;
    private BigDecimal monthlySalesAmount;
    private Integer monthlyOrderCount;
    private String hotProductSummary;
    private Integer viewCountSummary;
    private Integer favoriteCountSummary;
    private Integer repeatBuyerCount;
    private LocalDateTime lastCalculatedAt;

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public BigDecimal getMonthlySalesAmount() {
        return monthlySalesAmount;
    }

    public void setMonthlySalesAmount(BigDecimal monthlySalesAmount) {
        this.monthlySalesAmount = monthlySalesAmount;
    }

    public Integer getMonthlyOrderCount() {
        return monthlyOrderCount;
    }

    public void setMonthlyOrderCount(Integer monthlyOrderCount) {
        this.monthlyOrderCount = monthlyOrderCount;
    }

    public String getHotProductSummary() {
        return hotProductSummary;
    }

    public void setHotProductSummary(String hotProductSummary) {
        this.hotProductSummary = hotProductSummary;
    }

    public Integer getViewCountSummary() {
        return viewCountSummary;
    }

    public void setViewCountSummary(Integer viewCountSummary) {
        this.viewCountSummary = viewCountSummary;
    }

    public Integer getFavoriteCountSummary() {
        return favoriteCountSummary;
    }

    public void setFavoriteCountSummary(Integer favoriteCountSummary) {
        this.favoriteCountSummary = favoriteCountSummary;
    }

    public Integer getRepeatBuyerCount() {
        return repeatBuyerCount;
    }

    public void setRepeatBuyerCount(Integer repeatBuyerCount) {
        this.repeatBuyerCount = repeatBuyerCount;
    }

    public LocalDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }
}
