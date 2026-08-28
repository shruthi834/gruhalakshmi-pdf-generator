package com.example.pdfgen;

import com.example.pdfgen.dto.PdfRequest;
import com.example.pdfgen.service.PdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@SpringBootTest
class RenderPdfToImageTest {

    @Autowired
    private PdfService pdfService;

    @Test
    void renderPdfToPng() throws Exception {
        PdfRequest request = new PdfRequest("2026-08-25", "240199887766", "ಭಾಗ್ಯಮ್ಮ");
        byte[] pdfBytes = pdfService.generatePdf(request);

        File outDir = new File("/Users/shruthi/.gemini/antigravity-ide/brain/1d34670c-c1e3-432f-af70-0a738282d3ce/scratch");
        outDir.mkdirs();
        File outFile = new File(outDir, "generated_sample.png");

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage bim = renderer.renderImageWithDPI(0, 150);
            ImageIO.write(bim, "PNG", outFile);
        }
        System.out.println("Rendered sample PDF to: " + outFile.getAbsolutePath());
    }
}
