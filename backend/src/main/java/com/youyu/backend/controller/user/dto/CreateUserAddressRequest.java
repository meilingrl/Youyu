package com.youyu.backend.controller.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserAddressRequest {

    @NotBlank(message = "收件人不能为空")
    @Size(max = 64, message = "收件人不能超过64个字符")
    private String receiverName;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 32, message = "联系电话不能超过32个字符")
    private String receiverPhone;

    @Size(max = 32, message = "地址类型不能超过32个字符")
    private String addressType = "campus";

    private String province;
    private String city;
    private String district;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过255个字符")
    private String detailAddress;

    @Size(max = 128, message = "校区不能超过128个字符")
    private String campusArea;

    private boolean defaultAddress;

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getCampusArea() {
        return campusArea;
    }

    public void setCampusArea(String campusArea) {
        this.campusArea = campusArea;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
