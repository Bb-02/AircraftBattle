package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;

/**
 * 爆炸效果类（修正drawExplosion调用参数，实现动画）
 */
public class Explosion {
    private int x;          // 爆炸中心X坐标
    private int y;          // 爆炸中心Y坐标
    private int size;       // 爆炸尺寸（渐变增大）
    private long startTime; // 爆炸开始时间
    private static final long DURATION = 500; // 爆炸持续时间（500ms）

    public Explosion(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = 20; // 初始尺寸
        this.startTime = System.currentTimeMillis();
    }

    // 绘制爆炸效果（调用修复后的drawExplosion，参数匹配）
    public void draw(Graphics g) {
        if (!isExpired()) {
            // 爆炸尺寸随时间增大（动画效果）
            long elapsed = System.currentTimeMillis() - startTime;
            size = 20 + (int) (elapsed * 0.08); // 渐变增大
            // 调用drawExplosion：传入Graphics2D、x、y、size（4个参数，匹配方法定义）
            DrawUtil.drawExplosion((Graphics2D) g, x, y, size);
        }
    }

    // 检查爆炸是否过期（持续时间结束）
    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > DURATION;
    }

    // Getter
    public int getX() { return x; }
    public int getY() { return y; }
}