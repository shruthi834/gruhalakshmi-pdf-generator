package com.example.pdfgen.model;

public enum PaymentStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_AUTHORIZED,
    PAYMENT_CAPTURED,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    PDF_GENERATED,
    PDF_GENERATION_FAILED
}
