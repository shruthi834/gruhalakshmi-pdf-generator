package com.example.pdfgen.dto;

public class PaymentOrderResponse {

    private String orderId;
    private Integer amount; // In smallest currency unit (e.g. 10000 paise for ₹100)
    private String currency; // "INR"
    private String keyId; // Razorpay Key ID for frontend Checkout SDK

    public PaymentOrderResponse() {
    }

    public PaymentOrderResponse(String orderId, Integer amount, String currency, String keyId) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.keyId = keyId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }
}
