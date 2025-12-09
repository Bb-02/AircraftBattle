package com.aircraftwar.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片加载工具类
 */
public class ImageUtil {
    /**
     * 加载图片
     * @param path 图片路径
     * @return 加载后的图片
     */
    public static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            System.err.println("图片加载失败：" + path);
            e.printStackTrace();
            return null;
        }
    }
}