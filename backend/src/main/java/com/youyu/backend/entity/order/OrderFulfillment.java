package com.youyu.backend.entity.order;

import com.youyu.backend.entity.BaseEntity;
import java.time.LocalDateTime;

public class OrderFulfillment extends BaseEntity {

    private Long orderId;
    private String fulfillmentType;
    private String fulfillmentStatus;
    private LocalDateTime sellerConfirmedAt;
    private LocalDateTime buyerConfirmedAt;
    private String logisticsNo;
    private String logisticsCompany;
    private String offlineMeetingTime;
    private String offlineMeetingPlace;
    private LocalDateTime digitalAccessOpenedAt;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getFulfillmentType() { return fulfillmentType; }
    public void setFulfillmentType(String fulfillmentType) { this.fulfillmentType = fulfillmentType; }
    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }
    public LocalDateTime getSellerConfirmedAt() { return sellerConfirmedAt; }
    public void setSellerConfirmedAt(LocalDateTime sellerConfirmedAt) { this.sellerConfirmedAt = sellerConfirmedAt; }
    public LocalDateTime getBuyerConfirmedAt() { return buyerConfirmedAt; }
    public void setBuyerConfirmedAt(LocalDateTime buyerConfirmedAt) { this.buyerConfirmedAt = buyerConfirmedAt; }
    public String getLogisticsNo() { return logisticsNo; }
    public void setLogisticsNo(String logisticsNo) { this.logisticsNo = logisticsNo; }
    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String logisticsCompany) { this.logisticsCompany = logisticsCompany; }
    public String getOfflineMeetingTime() { return offlineMeetingTime; }
    public void setOfflineMeetingTime(String offlineMeetingTime) { this.offlineMeetingTime = offlineMeetingTime; }
    public String getOfflineMeetingPlace() { return offlineMeetingPlace; }
    public void setOfflineMeetingPlace(String offlineMeetingPlace) { this.offlineMeetingPlace = offlineMeetingPlace; }
    public LocalDateTime getDigitalAccessOpenedAt() { return digitalAccessOpenedAt; }
    public void setDigitalAccessOpenedAt(LocalDateTime digitalAccessOpenedAt) { this.digitalAccessOpenedAt = digitalAccessOpenedAt; }
}
