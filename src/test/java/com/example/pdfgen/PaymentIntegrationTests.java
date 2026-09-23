package com.example.pdfgen;

import com.example.pdfgen.controller.PdfController;
import com.example.pdfgen.dto.PdfRequest;
import com.example.pdfgen.model.Download;
import com.example.pdfgen.repository.DownloadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PaymentIntegrationTests {

    @Autowired
    private PdfController pdfController;

    @Autowired
    private DownloadRepository downloadRepository;

    @Test
    void testPdfGenerationAndDownloadSaved() {
        long initialCount = downloadRepository.count();

        PdfRequest request = new PdfRequest("2026-08-25", "240199887766", "ರಮ್ಯ");
        ResponseEntity<byte[]> response = pdfController.generatePdf(request, null, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 1000);

        long newCount = downloadRepository.count();
        assertEquals(initialCount + 1, newCount, "Download record should be saved to database");

        List<Download> all = downloadRepository.findAll();
        Download latest = all.get(all.size() - 1);
        assertNotNull(latest.getUser());
        assertEquals("PDF_SANCTION_ORDER", latest.getResourceType());
        assertNotNull(latest.getDownloadedAt());
    }
}
