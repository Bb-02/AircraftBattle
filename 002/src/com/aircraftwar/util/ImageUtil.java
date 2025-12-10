package com.aircraftwar.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片加载工具类（带详细调试日志，支持多路径加载）
 */
public class ImageUtil {
    private static Map<String, BufferedImage> imageCache = new HashMap<>();

    /**
     * 加载图片（优先类路径 → 再文件路径，打印详细日志）
     * @param fileName 图片文件名（如 PlayerPlane.png/Enemy1.png）
     * @return 加载后的图片，失败返回null
     */
    public static BufferedImage loadImage(String fileName) {
        // 1. 先查缓存
        if (imageCache.containsKey(fileName)) {
            System.out.println("[ImageUtil] 缓存命中：" + fileName);
            return imageCache.get(fileName);
        }

        BufferedImage image = null;
        String resourcePath = "images/" + fileName;

        // 2. 尝试「类路径加载」（适配IDEA/Eclipse运行）
        System.out.println("[ImageUtil] 尝试类路径加载：" + resourcePath);
        try (InputStream is = ImageUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.out.println("[ImageUtil] ❌ 类路径中找不到：" + resourcePath);
            } else {
                image = ImageIO.read(is);
                System.out.println("[ImageUtil] ✅ 类路径加载成功：" + fileName + "，尺寸：" + image.getWidth() + "x" + image.getHeight());
            }
        } catch (IOException e) {
            System.out.println("[ImageUtil] ❌ 类路径加载失败：" + fileName + "，原因：" + e.getMessage());
        }

        // 3. 类路径失败 → 尝试「项目根目录文件加载」
        if (image == null) {
            File imageFile = new File(resourcePath);
            System.out.println("[ImageUtil] 尝试文件路径加载：" + imageFile.getAbsolutePath());
            try {
                if (!imageFile.exists()) {
                    System.out.println("[ImageUtil] ❌ 文件不存在：" + imageFile.getAbsolutePath());
                } else {
                    image = ImageIO.read(imageFile);
                    System.out.println("[ImageUtil] ✅ 文件路径加载成功：" + fileName + "，尺寸：" + image.getWidth() + "x" + image.getHeight());
                }
            } catch (IOException e) {
                System.out.println("[ImageUtil] ❌ 文件路径加载失败：" + fileName + "，原因：" + e.getMessage());
            }
        }

        // 4. 缓存并返回
        if (image != null) {
            imageCache.put(fileName, image);
        } else {
            System.out.println("[ImageUtil] ❌ 所有加载方式都失败：" + fileName);
        }
        return image;
    }

    /**
     * 绘制图片（带降级日志）
     */
    public static void drawImage(Graphics2D g2d, BufferedImage image, int x, int y, int width, int height) {
        if (image != null) {
            g2d.drawImage(image, x, y, width, height, null);
        } else {
            System.out.println("[ImageUtil] ⚠️ 图片为null，降级绘制矩形：x=" + x + ", y=" + y + ", 尺寸=" + width + "x" + height);
            g2d.setColor(Color.RED);
            g2d.fillRect(x, y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, width, height);
        }
    }
}