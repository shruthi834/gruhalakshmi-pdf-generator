package com.example.pdfgen;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;

public class TestAwtKannada {

    @Test
    public void testAwtShaping() throws Exception {
        Font kannadaFont = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/NotoSansKannada-Bold.ttf")).deriveFont(Font.BOLD, 30f);

        String text = "ಶ್ರೀಮತಿ ಲಕ್ಷ್ಮಮ್ಮ";
        
        BufferedImage img = new BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 400, 100);
        
        g2d.setColor(new Color(33, 37, 41));
        g2d.setFont(kannadaFont);
        g2d.drawString(text, 20, 60);
        g2d.dispose();

        File out = new File("/Users/shruthi/.gemini/antigravity-ide/brain/1d34670c-c1e3-432f-af70-0a738282d3ce/scratch/awt_kannada_test.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("Saved awt_kannada_test.png");
    }
}
