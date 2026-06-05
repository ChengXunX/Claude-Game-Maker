package com.chengxun.gamemaker.web.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int CAPTCHA_LENGTH = 5;
    private static final int EXPIRE_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    // captchaId -> {code, expireTime}
    private final ConcurrentHashMap<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();

    public String generateCaptcha() {
        String captchaId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String code = generateCode();
        long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(EXPIRE_MINUTES);
        captchaStore.put(captchaId, new CaptchaEntry(code, expireAt));

        // 清理过期验证码
        captchaStore.entrySet().removeIf(e -> e.getValue().expireAt < System.currentTimeMillis());

        return captchaId;
    }

    public String generateCaptchaImageBase64(String captchaId) {
        CaptchaEntry entry = captchaStore.get(captchaId);
        if (entry == null) return null;

        try {
            BufferedImage image = createCaptchaImage(entry.code);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    public boolean verifyCaptcha(String captchaId, String inputCode) {
        if (captchaId == null || inputCode == null) return false;

        CaptchaEntry entry = captchaStore.remove(captchaId);
        if (entry == null) return false;

        if (entry.expireAt < System.currentTimeMillis()) return false;

        return entry.code.equalsIgnoreCase(inputCode.trim());
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage createCaptchaImage(String code) {
        int width = 150, height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // 干扰线
        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 6; i++) {
            int x1 = RANDOM.nextInt(width), y1 = RANDOM.nextInt(height);
            int x2 = RANDOM.nextInt(width), y2 = RANDOM.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            int x = RANDOM.nextInt(width), y = RANDOM.nextInt(height);
            g.setColor(randomColor());
            g.fillOval(x, y, 3, 3);
        }

        // 文字
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomDarkColor());
            int x = 10 + i * 28;
            int y = 32 + RANDOM.nextInt(8) - 4;
            double angle = Math.toRadians(RANDOM.nextInt(30) - 15);
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        g.dispose();
        return image;
    }

    private Color randomColor() {
        return new Color(RANDOM.nextInt(256), RANDOM.nextInt(256), RANDOM.nextInt(256));
    }

    private Color randomDarkColor() {
        return new Color(RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(100));
    }

    private static class CaptchaEntry {
        final String code;
        final long expireAt;

        CaptchaEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
