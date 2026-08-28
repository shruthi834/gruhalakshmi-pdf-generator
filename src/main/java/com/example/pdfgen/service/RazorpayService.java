package com.example.pdfgen.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private RazorpayClient client;

    public RazorpayService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret,
            @Value("${razorpay.webhook.secret}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;

        try {
            if (keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank()) {
                this.client = new RazorpayClient(keyId, keySecret);
            }
        } catch (Exception e) {
            log.warn("Could not initialize RazorpayClient: {}", e.getMessage());
        }
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    /**
     * Checks whether we are in active live API mode or mock/local test mode.
     */
    public boolean isLiveApiActive() {
        return client != null &&
               !keyId.startsWith("rzp_test_placeholder") &&
               !keyId.startsWith("rzp_test_mock") &&
               !keyId.equals("rzp_test_YOUR_KEY_ID") &&
               !keySecret.startsWith("lLoP0E6h4kX7xK2v9ZqT1Y8A") &&
               !keySecret.equals("YOUR_KEY_SECRET");
    }

    /**
     * Creates a Razorpay Order.
     *
     * @param amountInPaise Amount in smallest currency unit (e.g. 10000 paise for ₹100)
     * @param currency      Currency code (e.g. "INR")
     * @param receipt       Internal receipt identifier
     * @return Razorpay Order ID (e.g. "order_xxxxx")
     */
    public String createOrder(int amountInPaise, String currency, String receipt) throws Exception {
        if (isLiveApiActive()) {
            try {
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amountInPaise);
                orderRequest.put("currency", currency);
                orderRequest.put("receipt", receipt);
                orderRequest.put("payment_capture", 1); // Auto capture

                Order order = client.orders.create(orderRequest);
                return order.get("id");
            } catch (RazorpayException e) {
                log.error("Razorpay API order creation error: {}", e.getMessage());
                throw new Exception("Failed to create Razorpay payment order: " + e.getMessage(), e);
            }
        } else {
            // Local dev / test fallback: Generate valid mock Razorpay Order ID
            String mockOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("Test/Local mode: generated order ID: {}", mockOrderId);
            return mockOrderId;
        }
    }

    /**
     * Verifies the cryptographic HMAC-SHA256 signature returned by Razorpay Checkout.
     * HMAC_SHA256( razorpayOrderId + "|" + razorpayPaymentId, RAZORPAY_KEY_SECRET )
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String receivedSignature) {
        if (orderId == null || paymentId == null || receivedSignature == null) {
            return false;
        }

        try {
            String data = orderId + "|" + paymentId;
            String generatedSignature = calculateHmacSha256(data, keySecret);
            return MessageDigest.isEqual(
                    generatedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Queries Razorpay API to verify that the payment is in "captured" status,
     * matches the expected amount, currency, and belongs to the expected order ID.
     */
    public boolean verifyPaymentCapturedWithRazorpayApi(String orderId, String paymentId, int expectedAmount, String expectedCurrency) {
        if (isLiveApiActive()) {
            try {
                Payment payment = client.payments.fetch(paymentId);
                if (payment == null) {
                    log.error("Payment not found on Razorpay for id: {}", paymentId);
                    return false;
                }

                String status = payment.get("status");
                int amount = payment.get("amount");
                String currency = payment.get("currency");
                String paymentOrderId = payment.get("order_id");

                boolean isCaptured = "captured".equalsIgnoreCase(status);
                boolean isAmountMatch = (amount == expectedAmount);
                boolean isCurrencyMatch = expectedCurrency.equalsIgnoreCase(currency);
                boolean isOrderMatch = orderId.equals(paymentOrderId);

                if (!isCaptured) {
                    log.warn("Payment {} is not captured, status: {}", paymentId, status);
                }
                if (!isAmountMatch) {
                    log.warn("Payment {} amount mismatch: expected {}, got {}", paymentId, expectedAmount, amount);
                }
                if (!isOrderMatch) {
                    log.warn("Payment {} order mismatch: expected {}, got {}", paymentId, orderId, paymentOrderId);
                }

                return isCaptured && isAmountMatch && isCurrencyMatch && isOrderMatch;
            } catch (RazorpayException e) {
                log.error("Error fetching payment from Razorpay API: {}", e.getMessage(), e);
                return false;
            }
        } else {
            // In test/mock mode, signature verification is the source of truth
            log.info("Test/Local mode: Verified payment {} for order {}", paymentId, orderId);
            return true;
        }
    }

    /**
     * Verifies the Webhook signature against the configured webhook secret.
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (payload == null || signatureHeader == null || webhookSecret == null) {
            return false;
        }

        try {
            String expectedSignature = calculateHmacSha256(payload, webhookSecret);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Helper to compute standard HMAC-SHA256 hex string.
     */
    public static String calculateHmacSha256(String data, String secret) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
