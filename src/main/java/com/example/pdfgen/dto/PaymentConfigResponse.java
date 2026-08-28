package com.example.pdfgen.dto;

public class PaymentConfigResponse {

    private Integer amount; // In rupees (e.g. 100)
    private Integer amountPaise; // In paise (e.g. 10000)
    private String currency; // "INR"
    private String keyId; // Razorpay Key ID

    public PaymentConfigResponse() {
    }

    public PaymentConfigResponse(Integer amount, Integer amountPaise, String currency, String keyId) {
        this.amount = amount;
        this.amountPaise = amountPaise;
        this.currency = currency;
        this.keyId = keyId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Integer amountPaise) {
        this.amountPaise = amountPaise;
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
