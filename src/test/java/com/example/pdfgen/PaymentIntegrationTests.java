package com.example.pdfgen;

import com.example.pdfgen.dto.PaymentOrderResponse;
import com.example.pdfgen.dto.PaymentVerifyRequest;
import com.example.pdfgen.dto.PdfRequest;
import com.example.pdfgen.model.PaymentOrder;
import com.example.pdfgen.model.PaymentStatus;
import com.example.pdfgen.repository.PaymentOrderRepository;
import com.example.pdfgen.service.PaymentService;
import com.example.pdfgen.service.RazorpayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PaymentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RazorpayService razorpayService;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        paymentOrderRepository.deleteAll();
    }

    @Test
    void testPaymentConfigEndpoint() throws Exception {
        mockMvc.perform(get("/api/payment/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.amountPaise").value(10000))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.keyId").exists());
    }

    @Test
    void testCreatePaymentOrderSuccessfully() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಲಕ್ಷ್ಮಮ್ಮ");

        String responseBody = mockMvc.perform(post("/api/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.amount").value(10000)) // ₹100 = 10000 paise
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.keyId").exists())
                .andReturn().getResponse().getContentAsString();

        PaymentOrderResponse response = objectMapper.readValue(responseBody, PaymentOrderResponse.class);
        assertNotNull(response.getOrderId());

        PaymentOrder savedOrder = paymentOrderRepository.findByRazorpayOrderId(response.getOrderId()).orElse(null);
        assertNotNull(savedOrder);
        assertEquals(PaymentStatus.CREATED, savedOrder.getStatus());
        assertEquals("240100159730", savedOrder.getRationCardNumber());
        assertEquals("ಲಕ್ಷ್ಮಮ್ಮ", savedOrder.getName());
    }

    @Test
    void testMissingNameFailsValidation() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "");

        mockMvc.perform(post("/api/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingDateFailsValidation() throws Exception {
        PdfRequest request = new PdfRequest("", "240100159730", "ಶಿವಮ್ಮ");

        mockMvc.perform(post("/api/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingRationCardFailsValidation() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "", "ಶಿವಮ್ಮ");

        mockMvc.perform(post("/api/payment/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBackendEnforcesConfiguredAmount() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶಿವಮ್ಮ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        // Verify the amount is 10000 paise regardless of what frontend would send
        assertEquals(10000, order.getAmount());
        assertEquals("INR", order.getCurrency());
    }

    @Test
    void testSuccessfulSignatureVerificationAndPdfGeneration() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶಿವಮ್ಮ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        String paymentId = "pay_test_" + System.currentTimeMillis();
        String validSignature = RazorpayService.calculateHmacSha256(
                order.getOrderId() + "|" + paymentId,
                razorpayService.getKeySecret()
        );

        PaymentVerifyRequest verifyRequest = new PaymentVerifyRequest(
                order.getOrderId(),
                paymentId,
                validSignature
        );

        byte[] pdfBytes = mockMvc.perform(post("/api/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 10000);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, doc.getNumberOfPages());
        }

        PaymentOrder updatedOrder = paymentOrderRepository.findByRazorpayOrderId(order.getOrderId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(PaymentStatus.PDF_GENERATED, updatedOrder.getStatus());
        assertEquals(paymentId, updatedOrder.getRazorpayPaymentId());
    }

    @Test
    void testInvalidSignatureRejection() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶಿವಮ್ಮ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        String paymentId = "pay_test_" + System.currentTimeMillis();
        String invalidSignature = "invalid_signature_hex_code_12345";

        PaymentVerifyRequest verifyRequest = new PaymentVerifyRequest(
                order.getOrderId(),
                paymentId,
                invalidSignature
        );

        mockMvc.perform(post("/api/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment signature verification failed"));

        PaymentOrder updatedOrder = paymentOrderRepository.findByRazorpayOrderId(order.getOrderId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(PaymentStatus.PAYMENT_FAILED, updatedOrder.getStatus());
    }

    @Test
    void testWrongOrderIdRejection() throws Exception {
        String invalidOrderId = "order_non_existent_123";
        String paymentId = "pay_test_123";
        String signature = "some_signature";

        PaymentVerifyRequest verifyRequest = new PaymentVerifyRequest(
                invalidOrderId,
                paymentId,
                signature
        );

        mockMvc.perform(post("/api/payment/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDuplicatePaymentVerificationIsIdempotent() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶ್ರುತಿ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        String paymentId = "pay_test_" + System.currentTimeMillis();
        String validSignature = RazorpayService.calculateHmacSha256(
                order.getOrderId() + "|" + paymentId,
                razorpayService.getKeySecret()
        );

        PaymentVerifyRequest verifyRequest = new PaymentVerifyRequest(
                order.getOrderId(),
                paymentId,
                validSignature
        );

        // First verification
        byte[] pdf1 = paymentService.verifyAndProcessPayment(verifyRequest);
        assertNotNull(pdf1);

        // Duplicate verification should succeed idempotently and return PDF without error or duplicate charges
        byte[] pdf2 = paymentService.verifyAndProcessPayment(verifyRequest);
        assertNotNull(pdf2);
        assertEquals(pdf1.length, pdf2.length);
    }

    @Test
    void testPdfCannotBeDownloadedBeforePayment() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶ್ರುತಿ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        // Attempting to download PDF when status is CREATED (unpaid) must fail with 403 Forbidden
        mockMvc.perform(get("/api/payment/download/" + order.getOrderId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPdfCanBeDownloadedAfterPaymentWithoutRecharge() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶ್ರುತಿ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        String paymentId = "pay_test_" + System.currentTimeMillis();
        String validSignature = RazorpayService.calculateHmacSha256(
                order.getOrderId() + "|" + paymentId,
                razorpayService.getKeySecret()
        );

        paymentService.verifyAndProcessPayment(new PaymentVerifyRequest(order.getOrderId(), paymentId, validSignature));

        // Downloading the PDF via /api/payment/download/{orderId} succeeds
        byte[] pdfBytes = mockMvc.perform(get("/api/payment/download/" + order.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 10000);
    }

    @Test
    void testWebhookIdempotency() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240100159730", "ಶ್ರುತಿ");
        PaymentOrderResponse order = paymentService.createPaymentOrder(request);

        String paymentId = "pay_webhook_test_" + System.currentTimeMillis();
        String webhookPayload = """
            {
                "event": "payment.captured",
                "payload": {
                    "payment": {
                        "entity": {
                            "id": "%s",
                            "order_id": "%s",
                            "status": "captured",
                            "amount": 10000,
                            "currency": "INR"
                        }
                    }
                }
            }
        """.formatted(paymentId, order.getOrderId());

        String webhookSignature = RazorpayService.calculateHmacSha256(
                webhookPayload,
                razorpayService.getWebhookSecret()
        );

        // First webhook delivery
        mockMvc.perform(post("/api/payment/webhook")
                        .header("X-Razorpay-Signature", webhookSignature)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        PaymentOrder capturedOrder = paymentOrderRepository.findByRazorpayOrderId(order.getOrderId()).orElse(null);
        assertNotNull(capturedOrder);
        assertEquals(PaymentStatus.PAYMENT_CAPTURED, capturedOrder.getStatus());
        assertEquals(paymentId, capturedOrder.getRazorpayPaymentId());

        // Duplicate webhook delivery should succeed idempotently
        mockMvc.perform(post("/api/payment/webhook")
                        .header("X-Razorpay-Signature", webhookSignature)
                        .content(webhookPayload))
                .andExpect(status().isOk());
    }
}
