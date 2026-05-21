package com.campusmarket.backend.entity.order;

import com.campusmarket.backend.entity.BaseEntity;
import java.math.BigDecimal;

public class OrderItem extends BaseEntity {

    private Long orderId;
    private Long productId;
    private String titleSnapshot;
    private String imageSnapshot;
    private BigDecimal priceSnapshot;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private String productTypeSnapshot;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getTitleSnapshot() { return titleSnapshot; }
    public void setTitleSnapshot(String titleSnapshot) { this.titleSnapshot = titleSnapshot; }
    public String getImageSnapshot() { return imageSnapshot; }
    public void setImageSnapshot(String imageSnapshot) { this.imageSnapshot = imageSnapshot; }
    public BigDecimal getPriceSnapshot() { return priceSnapshot; }
    public void setPriceSnapshot(BigDecimal priceSnapshot) { this.priceSnapshot = priceSnapshot; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public String getProductTypeSnapshot() { return productTypeSnapshot; }
    public void setProductTypeSnapshot(String productTypeSnapshot) { this.productTypeSnapshot = productTypeSnapshot; }
}
