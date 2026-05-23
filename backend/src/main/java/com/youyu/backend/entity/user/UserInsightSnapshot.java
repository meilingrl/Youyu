package com.youyu.backend.entity.user;

import com.youyu.backend.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserInsightSnapshot extends BaseEntity {

    private Long userId;
    private BigDecimal totalSpendAmount;
    private Integer totalPurchasedItemCount;
    private Integer recentBrowseCount;
    private String favoriteCategorySummary;
    private LocalDateTime lastCalculatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalSpendAmount() {
        return totalSpendAmount;
    }

    public void setTotalSpendAmount(BigDecimal totalSpendAmount) {
        this.totalSpendAmount = totalSpendAmount;
    }

    public Integer getTotalPurchasedItemCount() {
        return totalPurchasedItemCount;
    }

    public void setTotalPurchasedItemCount(Integer totalPurchasedItemCount) {
        this.totalPurchasedItemCount = totalPurchasedItemCount;
    }

    public Integer getRecentBrowseCount() {
        return recentBrowseCount;
    }

    public void setRecentBrowseCount(Integer recentBrowseCount) {
        this.recentBrowseCount = recentBrowseCount;
    }

    public String getFavoriteCategorySummary() {
        return favoriteCategorySummary;
    }

    public void setFavoriteCategorySummary(String favoriteCategorySummary) {
        this.favoriteCategorySummary = favoriteCategorySummary;
    }

    public LocalDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }
}
