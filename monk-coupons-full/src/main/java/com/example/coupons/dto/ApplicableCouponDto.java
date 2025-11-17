package com.example.coupons.dto;

public class ApplicableCouponDto {
    private String couponId;
    private String type;
    private double discount;

    public ApplicableCouponDto() {}
    public ApplicableCouponDto(String couponId, String type, double discount) {
        this.couponId = couponId; this.type = type; this.discount = discount;
    }
    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
}
