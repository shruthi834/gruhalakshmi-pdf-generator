package com.example.pdfgen;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;

class ExtractImagesTest {

    @Test
    void extractImages() throws Exception {
        byte[] pdfBytes = new ClassPathResource("templates/gruhalakshmi_template.pdf").getInputStream().readAllBytes();
        File outDir = new File("src/main/resources/static/images");
        outDir.mkdirs();
        File pubDir = new File("frontend/public/images");
        pubDir.mkdirs();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDPage page = doc.getPage(0);
            PDResources resources = page.getResources();

            for (COSName name : resources.getXObjectNames()) {
                PDXObject xobject = resources.getXObject(name);
                if (xobject instanceof PDImageXObject img) {
                    BufferedImage bi = img.getImage();
                    String imgName = name.getName() + ".png";
                    ImageIO.write(bi, "PNG", new File(outDir, imgName));
                    ImageIO.write(bi, "PNG", new File(pubDir, imgName));
                    
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bi, "PNG", baos);
                    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                    System.out.println("Extracted " + imgName + " (" + bi.getWidth() + "x" + bi.getHeight() + "), Base64 length: " + b64.length());
                }
            }
        }
    }
}
