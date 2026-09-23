package com.example.pdfgen.service;

import com.example.pdfgen.dto.*;
import com.example.pdfgen.model.PaymentOrder;
import com.example.pdfgen.model.PaymentStatus;
import com.example.pdfgen.repository.PaymentOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PdfService pdfService;

    public PaymentService(PaymentOrderRepository paymentOrderRepository,
                          PdfService pdfService) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.pdfService = pdfService;
    }

    /**
     * This method is no longer supported after removal of Razorpay integration.
     */
    public PaymentConfigResponse getPaymentConfig() {
        throw new UnsupportedOperationException("Payment config is not supported after Razorpay removal.");
    }

    /**
     * Creation of Razorpay payment orders is disabled. The new flow uses user sessions.
     */
    @Transactional
    public PaymentOrderResponse createPaymentOrder(PdfRequest request) {
        throw new UnsupportedOperationException("Payment order creation is disabled – use the new session‑based flow.");
    }

    /**
     * Verification of Razorpay payment signatures is no longer applicable.
     */
    @Transactional(noRollbackFor = {SecurityException.class, IllegalStateException.class})
    public byte[] verifyAndProcessPayment(PaymentVerifyRequest request) {
        throw new UnsupportedOperationException("Payment verification is disabled – PDFs are now gated by user sessions.");
    }

    /**
     * Downloads / Generates the PDF for an already verified & captured payment order.
     * This method remains functional for legacy data but does not invoke Razorpay services.
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
     * Webhook processing is no longer supported after Razorpay removal.
     */
    @Transactional
    public void processWebhook(String payload, String signatureHeader) {
        throw new UnsupportedOperationException("Razorpay webhook handling has been removed.");
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

