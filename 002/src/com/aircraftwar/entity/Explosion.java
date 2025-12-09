package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;

/**
 * 爆炸效果类（纯代码绘图）
 */
public class Explosion {
    private int x;
    private int y;
    private int size = 50; // 爆炸尺寸
    private long createTime;
    private static final long DURATION = 500; // 显示时长500毫秒

    public Explosion(int x, int y) {
        this.x = x;
        this.y = y;
        this.createTime = System.currentTimeMillis();
    }

    // 绘制爆炸效果
    public void draw(Graphics g) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - createTime < DURATION) {
            // 爆炸效果随时间放大
            int currentSize = size + (int)((currentTime - createTime) / (double)DURATION * 20);
            DrawUtil.drawExplosion((Graphics2D) g, x + 20, y + 20, currentSize);
        }
    }

    // 判断是否过期
    public boolean isExpired() {
        return System.currentTimeMillis() - createTime >= DURATION;
    }
}