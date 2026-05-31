package com.youyu.backend.service.auth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class CaptchaImageRenderer {

    public String renderDataUrl(String code) {
        BufferedImage image = new BufferedImage(144, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(246, 249, 255));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            for (int index = 0; index < code.length(); index++) {
                graphics.setColor(new Color(32 + index * 22, 72, 135 + index * 12));
                graphics.drawString(String.valueOf(code.charAt(index)), 18 + index * 29, 34 - (index % 2) * 4);
            }
            graphics.setColor(new Color(120, 145, 180));
            graphics.drawLine(8, 16, 136, 31);
            graphics.drawLine(12, 38, 132, 10);
        } finally {
            graphics.dispose();
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to render CAPTCHA image", exception);
        }
    }
}
