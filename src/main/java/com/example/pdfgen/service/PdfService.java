package com.example.pdfgen.service;

import com.example.pdfgen.dto.PdfRequest;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class PdfService {

    private static final String TEMPLATE_PATH = "templates/gruhalakshmi_template.pdf";
    private static final String NIRMALA_REGULAR_FONT = "fonts/Nirmala.ttf";
    private static final String NIRMALA_BOLD_FONT = "fonts/NirmalaB.ttf";
    private static final String SEGOE_REGULAR_FONT = "fonts/segoeui.ttf";
    private static final String SEGOE_BOLD_FONT = "fonts/segoeuib.ttf";

    public byte[] generatePdf(PdfRequest request) throws Exception {
        byte[] templateBytes;
        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            templateBytes = is.readAllBytes();
        }

        try (PDDocument document = Loader.loadPDF(templateBytes)) {
            PDPage page = document.getPage(0);

            // Load exact Nirmala UI and Segoe UI fonts matching original template
            Font kannadaRegular;
            try (InputStream is = new ClassPathResource(NIRMALA_REGULAR_FONT).getInputStream()) {
                kannadaRegular = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 15f);
            }

            Font kannadaBold;
            try (InputStream is = new ClassPathResource(NIRMALA_BOLD_FONT).getInputStream()) {
                kannadaBold = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.BOLD, 15f);
            }

            Font segoeBold;
            try (InputStream is = new ClassPathResource(SEGOE_BOLD_FONT).getInputStream()) {
                segoeBold = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.BOLD, 15f);
            }

            Font segoeRegular;
            try (InputStream is = new ClassPathResource(SEGOE_REGULAR_FONT).getInputStream()) {
                segoeRegular = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 15f);
            }

            PDFont segoeRegularPdfFont;
            try (InputStream is = new ClassPathResource(SEGOE_REGULAR_FONT).getInputStream()) {
                segoeRegularPdfFont = PDType0Font.load(document, is);
            }

            PDFont segoeBoldPdfFont;
            try (InputStream is = new ClassPathResource(SEGOE_BOLD_FONT).getInputStream()) {
                segoeBoldPdfFont = PDType0Font.load(document, is);
            }

            // Extract & sanitize dynamic values
            String rawDate = request.getDate() != null ? request.getDate().trim() : "2023-07-21";
            String formattedDate = normalizeDate(rawDate);
            String rcNumber = request.getRationCardNumber() != null ? request.getRationCardNumber().trim() : "240100159730";
            String name = request.getName() != null ? request.getName().trim() : "ಶಿವಮ್ಮ";
            String footerDate = formatFooterDate(rawDate);

            // Generate dynamic QR Code containing strictly the 3 fields
            String qrData = "Date: " + formattedDate + "\n" +
                            "Ration Card No: " + rcNumber + "\n" +
                            "Name: " + name;

            BufferedImage qrImage = generateQrImage(qrData, 300, 300);
            PDImageXObject qrPdImage = LosslessFactory.createFromImage(document, qrImage);

            // =========================================================================
            // Render the 3-line body paragraph with Nirmala UI and Segoe UI fonts
            // Stay safely inside the borders (x from 40.0 to 770.0, borders at 36.0 and 780.45)
            // =========================================================================
            float scale = 4.0f; // 288 DPI crystal clarity
            float pdfX = 40.0f;
            float pdfY = 680.0f;
            float pdfW = 730.0f;
            float pdfH = 80.0f;

            int imgW = (int) (pdfW * scale);
            int imgH = (int) (pdfH * scale);

            BufferedImage paraImg = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = paraImg.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Pure white background covering template lines seamlessly without touching outer borders
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, imgW, imgH);

            g2d.scale(scale, scale);
            g2d.setColor(new Color(33, 37, 41)); // #212529

            // Line 1: Baseline at y = 20.75f (maps to PDF y = 739.25f)
            float y1 = 20.75f;
            float curX = 0f;
            curX = drawAwtSegment(g2d, kannadaRegular, "ಪಡಿತರ ಚೀಟಿ ಸಂಖ್ಯೆ ", curX, y1, false);
            curX = drawAwtSegment(g2d, segoeBold, rcNumber, curX, y1, true);
            curX = drawAwtSegment(g2d, kannadaRegular, " ಹೊಂದಿರುವ ಶ್ರೀಮತಿ ", curX, y1, false);
            Font nFont = isKannada(name) ? kannadaBold : segoeBold;
            curX = drawAwtSegment(g2d, nFont, name, curX, y1, true);
            curX = drawAwtSegment(g2d, kannadaRegular, " ರವರ ಗೃಹಲಕ್ಷ್ಮಿ ಯೋಜನೆಯ ಅರ್ಜಿಯನ್ನು", curX, y1, false);

            // Line 2: Baseline at y = 40.75f (maps to PDF y = 719.25f)
            float y2 = 40.75f;
            curX = 0f;
            curX = drawAwtSegment(g2d, kannadaRegular, "ದಿನಾಂಕ ", curX, y2, false);
            curX = drawAwtSegment(g2d, segoeBold, formattedDate, curX, y2, true);
            curX = drawAwtSegment(g2d, kannadaRegular, " ರಂದು ಒಪ್ಪಿ ಮಂಜೂರು ಮಾಡಲಾಗಿದೆ. ಗೃಹಲಕ್ಷ್ಮಿ ಯೋಜನೆಯ ನಗದು ಸೌಲಭ್ಯವನ್ನು", curX, y2, false);

            // Line 3: Baseline at y = 60.75f (maps to PDF y = 699.25f)
            float y3 = 60.75f;
            drawAwtSegment(g2d, kannadaRegular, "ಆಧಾರ್ ಲಿಂಕ್ ಆದ ಬ್ಯಾಂಕ್ ಖಾತೆಗೆ ವರ್ಗಾಯಿಸಲಾಗುವುದು.", 0f, y3, false);

            g2d.dispose();

            PDImageXObject paraPdImage = LosslessFactory.createFromImage(document, paraImg);

            Color textColor = new Color(33, 37, 41);
            Color darkBlack = new Color(0, 0, 0);

            try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                // 1. Draw shaped 3-line paragraph overlay
                cs.drawImage(paraPdImage, pdfX, pdfY, pdfW, pdfH);

                // 2. Explicitly restore / guarantee outer border line continuity
                cs.saveGraphicsState();
                cs.setStrokingColor(Color.BLACK);
                cs.setLineWidth(1.0f);
                // Right vertical border line
                cs.moveTo(780.45f, 437.25f);
                cs.lineTo(780.45f, 1104.75f);
                cs.stroke();
                cs.restoreGraphicsState();

                // 3. Order Number (GL- ...) at bottom of box
                maskArea(cs, 225f, 508f, 250f, 22f);
                String orderText = "GL- " + rcNumber;
                drawText(cs, segoeBoldPdfFont, 15f, textColor, 228f, 518.25f, orderText);

                // 4. Dynamic QR Code strictly with the 3 fields
                maskArea(cs, 666f, 472f, 104f, 104f);
                cs.drawImage(qrPdImage, 668.887f, 474.75f, 99f, 99f);

                // 5. Header URL (Top Right)
                maskArea(cs, 490f, 1175f, 350f, 16f);
                String fullHeaderUrl = "https://sevasindhugs1.karnataka.gov.in/gl-step/auth/print_order.php?rc=" + rcNumber;
                drawText(cs, segoeRegularPdfFont, 10f, darkBlack, 494.85f, 1182.0f, fullHeaderUrl);

                // 6. Footer Date (Bottom Right)
                maskArea(cs, 740f, 0f, 100f, 16f);
                drawText(cs, segoeRegularPdfFont, 10f, darkBlack, 755f, 2.45f, footerDate);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private float drawAwtSegment(Graphics2D g2d, Font font, String text, float x, float y, boolean underline) {
        FontRenderContext frc = g2d.getFontRenderContext();
        TextLayout layout = new TextLayout(text, font, frc);
        layout.draw(g2d, x, y);
        float width = (float) layout.getAdvance();

        if (underline) {
            Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawLine((int) x, (int) (y + 3), (int) (x + width), (int) (y + 3));
            g2d.setStroke(oldStroke);
        }

        return x + width;
    }

    private void maskArea(PDPageContentStream cs, float x, float y, float w, float h) throws Exception {
        cs.saveGraphicsState();
        cs.setNonStrokingColor(Color.WHITE);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.restoreGraphicsState();
    }

    private void drawText(PDPageContentStream cs, PDFont font, float fontSize, Color color, float x, float y, String text) throws Exception {
        cs.saveGraphicsState();
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        cs.restoreGraphicsState();
    }

    private boolean isKannada(String text) {
        if (text == null) return false;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KANNADA) {
                return true;
            }
        }
        return false;
    }

    private String normalizeDate(String input) {
        if (input == null || input.isBlank()) {
            return "21-07-2023";
        }
        input = input.trim();
        if (input.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            String[] parts = input.split("-");
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }
        return input;
    }

    private String formatFooterDate(String inputDate) {
        try {
            LocalDate date;
            if (inputDate != null && inputDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                date = LocalDate.parse(inputDate);
            } else if (inputDate != null && inputDate.matches("^\\d{2}-\\d{2}-\\d{4}$")) {
                date = LocalDate.parse(inputDate, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } else {
                date = LocalDate.now();
            }
            return DateTimeFormatter.ofPattern("M/d/yyyy, 5:19 PM", Locale.ENGLISH).format(date);
        } catch (Exception e) {
            return "7/21/2023, 5:19 PM";
        }
    }

    private BufferedImage generateQrImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
