package com.example.pdfgen;

import com.example.pdfgen.dto.PdfRequest;
import com.example.pdfgen.service.PdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PdfGeneratorApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PdfService pdfService;

    @Test
    void testTemplateDefaultsEndpoint() throws Exception {
        mockMvc.perform(get("/api/pdf/template-defaults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rationCardNumber").value("240100159730"))
                .andExpect(jsonPath("$.name").value("ಶಿವಮ್ಮ"))
                .andExpect(jsonPath("$.date").value("2023-07-21"));
    }

    @Test
    void testPdfServiceGeneratesValidPdf() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240199887766", "ಭಾಗ್ಯಮ್ಮ");
        byte[] pdfBytes = pdfService.generatePdf(request);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100000);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void testEnglishNameSupportInPdfService() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "240199887766", "SHIVAMMA");
        byte[] bytes = pdfService.generatePdf(request);
        assertNotNull(bytes);
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void testLakshmammaKannadaInPdfService() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-27", "24010015973099", "ಲಕ್ಷ್ಮಮ್ಮ");
        byte[] bytes = pdfService.generatePdf(request);
        assertNotNull(bytes);
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
