package com.example.pdfgen.service;

import com.example.pdfgen.dto.*;
import com.example.pdfgen.model.PaymentOrder;
import com.example.pdfgen.model.PaymentStatus;
import com.example.pdfgen.repository.PaymentOrderRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final RazorpayService razorpayService;
    private final PdfService pdfService;

    private final int configuredAmountRupees;
    private final String configuredCurrency;

    public PaymentService(
            PaymentOrderRepository paymentOrderRepository,
            RazorpayService razorpayService,
            PdfService pdfService,
            @Value("${payment.amount:100}") int configuredAmountRupees,
            @Value("${payment.currency:INR}") String configuredCurrency) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.razorpayService = razorpayService;
        this.pdfService = pdfService;
        this.configuredAmountRupees = configuredAmountRupees;
        this.configuredCurrency = configuredCurrency;
    }

    public PaymentConfigResponse getPaymentConfig() {
        int amountPaise = configuredAmountRupees * 100;
        return new PaymentConfigResponse(
                configuredAmountRupees,
                amountPaise,
                configuredCurrency,
                razorpayService.getKeyId()
        );
    }

    /**
     * Creates a new Razorpay payment order for the requested document values.
     * The amount is strictly determined by the server configuration (e.g. ₹100 = 10000 paise).
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(PdfRequest request) throws Exception {
        if (request.getDate() == null || request.getDate().isBlank()) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.getRationCardNumber() == null || request.getRationCardNumber().isBlank()) {
            throw new IllegalArgumentException("Ration Card Number is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        int amountInPaise = configuredAmountRupees * 100;
        String receipt = "rc_" + request.getRationCardNumber().trim() + "_" + System.currentTimeMillis();

        String razorpayOrderId = razorpayService.createOrder(amountInPaise, configuredCurrency, receipt);

        PaymentOrder paymentOrder = new PaymentOrder(
                razorpayOrderId,
                amountInPaise,
                configuredCurrency,
                request.getDate().trim(),
                request.getRationCardNumber().trim(),
                request.getName().trim(),
                PaymentStatus.CREATED
        );

        paymentOrderRepository.save(paymentOrder);
        log.info("Created payment order: {} for Ration Card: {}", razorpayOrderId, request.getRationCardNumber());

        return new PaymentOrderResponse(
                razorpayOrderId,
                amountInPaise,
                configuredCurrency,
                razorpayService.getKeyId()
        );
    }

    /**
     * Verifies the Razorpay payment signature & status, gates PDF generation,
     * and returns the generated PDF bytes on success.
     */
    @Transactional(noRollbackFor = {SecurityException.class, IllegalStateException.class})
    public byte[] verifyAndProcessPayment(PaymentVerifyRequest request) throws Exception {
        String orderId = request.getRazorpayOrderId();
        String paymentId = request.getRazorpayPaymentId();
        String signature = request.getRazorpaySignature();

        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found for ID: " + orderId));

        // Idempotency: If this payment was already verified and captured for this order
        if (order.getStatus() == PaymentStatus.PDF_GENERATED && paymentId.equals(order.getRazorpayPaymentId())) {
            log.info("Order {} already processed with payment {}. Returning existing PDF.", orderId, paymentId);
            return generatePdfForOrder(order);
        }

        // 1. Verify Cryptographic Signature
        boolean isValidSignature = razorpayService.verifyPaymentSignature(orderId, paymentId, signature);
        if (!isValidSignature) {
            order.setStatus(PaymentStatus.PAYMENT_FAILED);
            paymentOrderRepository.save(order);
            log.error("Invalid payment signature for order: {}, payment: {}", orderId, paymentId);
            throw new SecurityException("Payment signature verification failed");
        }

        // 2. Verify Payment Capture Status, Amount, and Order association with Razorpay API
        boolean isCaptured = razorpayService.verifyPaymentCapturedWithRazorpayApi(
                orderId, paymentId, order.getAmount(), order.getCurrency()
        );
        if (!isCaptured) {
            order.setStatus(PaymentStatus.PAYMENT_FAILED);
            paymentOrderRepository.save(order);
            log.error("Payment {} not captured or amount mismatch for order: {}", paymentId, orderId);
            throw new IllegalStateException("Payment was not captured or does not match order parameters");
        }

        // 3. Mark payment as captured
        order.setRazorpayPaymentId(paymentId);
        order.setStatus(PaymentStatus.PAYMENT_CAPTURED);
        paymentOrderRepository.save(order);
        log.info("Payment successfully verified and captured for order: {}, payment: {}", orderId, paymentId);

        // 4. Generate PDF
        try {
            byte[] pdfBytes = generatePdfForOrder(order);
            order.setStatus(PaymentStatus.PDF_GENERATED);
            paymentOrderRepository.save(order);
            return pdfBytes;
        } catch (Exception e) {
            order.setStatus(PaymentStatus.PDF_GENERATION_FAILED);
            paymentOrderRepository.save(order);
            log.error("PDF generation failed after successful payment for order {}: {}", orderId, e.getMessage(), e);
            throw new Exception("Payment succeeded, but PDF generation encountered an issue. You can retry downloading without paying again.", e);
        }
    }

    /**
     * Downloads / Generates the PDF for an already verified & captured payment order.
     * Prevents charging the user again if PDF generation failed previously.
     */
    @Transactional(noRollbackFor = {Exception.class})
    public byte[] downloadPdfForOrder(String orderId) throws Exception {
        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found for ID: " + orderId));

        if (order.getStatus() != PaymentStatus.PAYMENT_CAPTURED &&
            order.getStatus() != PaymentStatus.PDF_GENERATED &&
            order.getStatus() != PaymentStatus.PDF_GENERATION_FAILED) {
            log.warn("Unauthorized attempt to download PDF for unverified order: {}, status: {}", orderId, order.getStatus());
            throw new SecurityException("Payment not completed or verified. Cannot generate PDF.");
        }

        try {
            byte[] pdfBytes = generatePdfForOrder(order);
            order.setStatus(PaymentStatus.PDF_GENERATED);
            paymentOrderRepository.save(order);
            return pdfBytes;
        } catch (Exception e) {
            order.setStatus(PaymentStatus.PDF_GENERATION_FAILED);
            paymentOrderRepository.save(order);
            throw e;
        }
    }

    /**
     * Retrieves status of a payment order.
     */
    public PaymentStatusResponse getPaymentStatus(String orderId) {
        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found for ID: " + orderId));

        return new PaymentStatusResponse(
                order.getRazorpayOrderId(),
                order.getRazorpayPaymentId(),
                order.getStatus(),
                order.getAmount(),
                order.getCurrency(),
                order.getDate(),
                order.getRationCardNumber(),
                order.getName()
        );
    }

    /**
     * Idempotent webhook processing for Razorpay events.
     */
    @Transactional
    public void processWebhook(String payload, String signatureHeader) {
        if (!razorpayService.verifyWebhookSignature(payload, signatureHeader)) {
            log.error("Invalid webhook signature received");
            throw new SecurityException("Invalid webhook signature");
        }

        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event");
            log.info("Processing Razorpay webhook event: {}", event);

            JSONObject payloadObj = json.optJSONObject("payload");
            if (payloadObj == null) return;

            JSONObject paymentObj = payloadObj.optJSONObject("payment");
            if (paymentObj != null) {
                JSONObject entity = paymentObj.optJSONObject("entity");
                if (entity != null) {
                    String orderId = entity.optString("order_id");
                    String paymentId = entity.optString("id");
                    String status = entity.optString("status");

                    if (orderId != null && !orderId.isBlank()) {
                        Optional<PaymentOrder> optOrder = paymentOrderRepository.findByRazorpayOrderId(orderId);
                        if (optOrder.isPresent()) {
                            PaymentOrder order = optOrder.get();
                            if ("captured".equalsIgnoreCase(status)) {
                                if (order.getStatus() != PaymentStatus.PDF_GENERATED && order.getStatus() != PaymentStatus.PAYMENT_CAPTURED) {
                                    order.setRazorpayPaymentId(paymentId);
                                    order.setStatus(PaymentStatus.PAYMENT_CAPTURED);
                                    paymentOrderRepository.save(order);
                                    log.info("Webhook updated order {} to PAYMENT_CAPTURED", orderId);
                                }
                            } else if ("failed".equalsIgnoreCase(status)) {
                                order.setStatus(PaymentStatus.PAYMENT_FAILED);
                                paymentOrderRepository.save(order);
                                log.info("Webhook updated order {} to PAYMENT_FAILED", orderId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
        }
    }

    private byte[] generatePdfForOrder(PaymentOrder order) throws Exception {
        PdfRequest pdfRequest = new PdfRequest(
                order.getDate(),
                order.getRationCardNumber(),
                order.getName()
        );
        return pdfService.generatePdf(pdfRequest);
    }
}
