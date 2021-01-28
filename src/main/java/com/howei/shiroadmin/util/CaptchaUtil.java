package com.howei.shiroadmin.util;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * 验证码工具类
 *
 * @author
 */
public class CaptchaUtil {
    private static final String RANDOM_STRS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String FONT_NAME = "Fixedsys";
    private static final int FONT_SIZE = 18;
    private Random random = new Random();
    private int width = 80;//图片高
    private int height = 25;//图片宽
    private int lineNum = 50;//干扰线数量
    private int strNum = 4;//字符数量

    public BufferedImage genRandomCodeImage(StringBuffer randomCode) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        Graphics graphics = image.getGraphics();
        //背景色
        graphics.setColor(getRandColor(200, 250));
        graphics.fillRect(0, 0, width, height);

        //干扰线颜色
        graphics.setColor(getRandColor(110, 120));
        //绘制干扰线
        for (int i = 0; i <= lineNum; i++) {
            drowLine(graphics);
        }
        //绘制随机字符
        graphics.setFont(new Font(FONT_NAME, Font.ROMAN_BASELINE, FONT_SIZE));
        for (int i = 0; i <= strNum; i++) {
            randomCode.append(drowString(graphics, i));
        }

        graphics.dispose();
        return image;
    }
    /**
     * 绘制字符串
     */
    private String drowString(Graphics graphics, int i) {
        graphics.setColor(new Color(random.nextInt(101), random.nextInt(111), random
                .nextInt(121)));
        String rand = String.valueOf(getRandomString(random.nextInt(RANDOM_STRS
                .length())));
        graphics.translate(random.nextInt(3), random.nextInt(3));
        graphics.drawString(rand, 13 * i, 16);
        return rand;
    }

    /**
     * 获取随机的字符
     */
    private String getRandomString(int num) {
        return String.valueOf(RANDOM_STRS.charAt(num));
    }

    /**
     * 绘制干扰线
     *
     * @param graphics
     */
    private void drowLine(Graphics graphics) {
        int x = random.nextInt(width);
        int y = random.nextInt(height);
        int x0 = random.nextInt(16);
        int y0 = random.nextInt(16);
        graphics.drawLine(x, y, x + x0, y + y0);


    }

    /**
     * @param fc
     * @param bc
     * @return 给定范围获得随机颜色
     */
    private Color getRandColor(int fc, int bc) {
        if (fc > 255) {
            fc = 255;
        }
        if (bc > 255) {
            bc = 255;
        }
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return new Color(r, g, b);
    }

}
