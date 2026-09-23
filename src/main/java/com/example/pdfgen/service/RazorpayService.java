package com.example.pdfgen.service;

/**
 * Stub implementation of RazorpayService retained only to satisfy compilation of legacy tests
 * after the real Razorpay integration has been removed. It provides minimal placeholder
 * behaviour for the static HMAC calculation and for retrieving key/secret values.
 */
public class RazorpayService {
    /**
     * Returns a dummy key secret. In production this would be the Razorpay key secret.
     */
    public String getKeySecret() {
        return "dummyKeySecret";
    }

    /**
     * Returns a dummy webhook secret.
     */
    public String getWebhookSecret() {
        return "dummyWebhookSecret";
    }

    /**
     * Calculates an HMAC‑SHA256 signature. This stub returns a constant placeholder value
     * sufficient for tests that only need a non‑null string.
     */
    public static String calculateHmacSha256(String data, String secret) {
        // Simple placeholder – real implementation would use javax.crypto.Mac.
        return "dummySignature";
    }
}
