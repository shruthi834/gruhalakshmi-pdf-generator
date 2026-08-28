package com.example.pdfgen.dto;

import com.example.pdfgen.model.PaymentStatus;

public class PaymentStatusResponse {

    private String orderId;
    private String paymentId;
    private PaymentStatus status;
    private Integer amount; // In rupees for user display (e.g. 100)
    private Integer amountPaise; // In paise (e.g. 10000)
    private String currency;
    private String date;
    private String rationCardNumber;
    private String name;
    private boolean canDownload;

    public PaymentStatusResponse() {
    }

    public PaymentStatusResponse(String orderId, String paymentId, PaymentStatus status, Integer amountPaise, String currency, String date, String rationCardNumber, String name) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.status = status;
        this.amountPaise = amountPaise;
        this.amount = amountPaise != null ? amountPaise / 100 : null;
        this.currency = currency;
        this.date = date;
        this.rationCardNumber = rationCardNumber;
        this.name = name;
        this.canDownload = (status == PaymentStatus.PAYMENT_CAPTURED || status == PaymentStatus.PDF_GENERATED || status == PaymentStatus.PDF_GENERATION_FAILED);
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRationCardNumber() {
        return rationCardNumber;
    }

    public void setRationCardNumber(String rationCardNumber) {
        this.rationCardNumber = rationCardNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCanDownload() {
        return canDownload;
    }

    public void setCanDownload(boolean canDownload) {
        this.canDownload = canDownload;
    }
}
