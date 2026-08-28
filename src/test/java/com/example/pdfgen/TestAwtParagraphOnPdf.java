package com.example.pdfgen;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;

public class TestAwtParagraphOnPdf {

    @Test
    public void testRenderFullParagraph() throws Exception {
        byte[] templateBytes = new ClassPathResource("templates/gruhalakshmi_template.pdf").getInputStream().readAllBytes();

        Font kannadaRegular = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/Nirmala.ttf")).deriveFont(Font.PLAIN, 15f);
        Font kannadaBold = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/NirmalaB.ttf")).deriveFont(Font.BOLD, 15f);
        Font segoeBold = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/segoeuib.ttf")).deriveFont(Font.BOLD, 15f);

        String rcNumber = "240100159730";
        String name = "ಶಿವಮ್ಮ";
        String date = "21-07-2023";

        // Keep pdfX and pdfW strictly inside the inner margin:
        // Left border is at x=36.0, right border is at x=780.45.
        // We set pdfX=39.0, pdfW=736.0 -> right edge is 775.0 (5.45 pt safely inside the border!)
        float scale = 4.0f;
        float pdfX = 39.0f;
        float pdfY = 680.0f;
        float pdfW = 736.0f;
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

        // Fill crisp white
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imgW, imgH);

        g2d.scale(scale, scale);
        g2d.setColor(new Color(33, 37, 41));

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
        curX = drawAwtSegment(g2d, segoeBold, date, curX, y2, true);
        curX = drawAwtSegment(g2d, kannadaRegular, " ರಂದು ಒಪ್ಪಿ ಮಂಜೂರು ಮಾಡಲಾಗಿದೆ. ಗೃಹಲಕ್ಷ್ಮಿ ಯೋಜನೆಯ ನಗದು ಸೌಲಭ್ಯವನ್ನು", curX, y2, false);

        // Line 3: Baseline at y = 60.75f (maps to PDF y = 699.25f)
        float y3 = 60.75f;
        drawAwtSegment(g2d, kannadaRegular, "ಆಧಾರ್ ಲಿಂಕ್ ಆದ ಬ್ಯಾಂಕ್ ಖಾತೆಗೆ ವರ್ಗಾಯಿಸಲಾಗುವುದು.", 0f, y3, false);

        g2d.dispose();

        try (PDDocument doc = Loader.loadPDF(templateBytes)) {
            PDPage page = doc.getPage(0);
            PDImageXObject pdImg = LosslessFactory.createFromImage(doc, paraImg);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                cs.drawImage(pdImg, pdfX, pdfY, pdfW, pdfH);
            }

            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage pageImg = renderer.renderImageWithDPI(0, 150);
            File out = new File("/Users/shruthi/.gemini/antigravity-ide/brain/1d34670c-c1e3-432f-af70-0a738282d3ce/scratch/border_check.png");
            ImageIO.write(pageImg, "PNG", out);
            System.out.println("Saved border_check.png");
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

    private boolean isKannada(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KANNADA) {
                return true;
            }
        }
        return false;
    }
}
