package com.example.pdfgen.controller;

import com.example.pdfgen.service.PaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the PDF download endpoint.
 *
 * After the authentication flow is added, this endpoint will be secured (e.g.,
 * with a Bearer token). For now it simply delegates to {@link PaymentService}.
 */
@RestController
@RequestMapping("/api/pdf")
public class PdfDownloadController {

    private final PaymentService paymentService;

    public PdfDownloadController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Download the generated PDF for a verified order.
     *
     * @param orderId the Razorpay order identifier (or any business order id)
     * @return the PDF bytes as an attachment
     */
    @GetMapping("/download/{orderId}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String orderId) throws Exception {
        byte[] pdfBytes = paymentService.downloadPdfForOrder(orderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
