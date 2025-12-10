package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;
// ========== 新增：导入图片工具类和图片类型 ==========
import com.aircraftwar.util.ImageUtil;
import java.awt.image.BufferedImage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 敌机类（适配小队初始位置 + 修复waveNumber未定义问题 + 替换为图片绘制）
 */
public class EnemyAircraft extends Aircraft {
    private static final Random random = new Random();
    // 敌机尺寸（保持原有尺寸，适配图片）
    private static final int ENEMY_WIDTH = 30;
    private static final int ENEMY_HEIGHT = 40;

    // ========== 新增：敌机图片对象 ==========
    private BufferedImage enemyImage;

    // 所属波次（关键修复）
    private int waveNumber;

    // 子弹相关
    private List<EnemyBullet> bullets;
    private long lastShootTime;
    private long shootInterval;

    // 移动相关
    private EnemyMoveType moveType;
    private int moveSpeed;

    // 逃跑相关
    private boolean isEscaping;
    private int escapeSpeed = 10;

    // 构造方法支持传入初始位置（核心：初始化waveNumber + 加载图片）
    public EnemyAircraft(int panelWidth, int panelHeight, EnemyMoveType moveType, int waveNumber, int initX, int initY) {
        super(initX, initY, 2 + waveNumber, 1, ENEMY_WIDTH, ENEMY_HEIGHT);

        this.waveNumber = waveNumber;

        // 加载敌机图片 + 日志
        System.out.println("[EnemyAircraft] 开始加载 Enemy1.png");
        this.enemyImage = ImageUtil.loadImage("Enemy1.png");
        if (this.enemyImage == null) {
            System.out.println("[EnemyAircraft] ❌ Enemy1.png 加载失败！");
        } else {
            System.out.println("[EnemyAircraft] ✅ Enemy1.png 加载成功！");
        }

        // 子弹初始化（原有逻辑）
        this.bullets = new ArrayList<>();
        this.shootInterval = 3000 - (waveNumber * 200);
        if (shootInterval < 800) {
            shootInterval = 800;
        }
        this.lastShootTime = System.currentTimeMillis();

        // 移动初始化（原有逻辑）
        this.moveType = moveType;
        this.moveSpeed = 2 + (waveNumber / 2);
        this.isEscaping = false;
    }

    // 兼容旧构造方法（避免报错）
    public EnemyAircraft(int panelWidth, int panelHeight, EnemyMoveType moveType, int waveNumber) {
        this(panelWidth, panelHeight, moveType, waveNumber,
                random.nextInt(Math.max(1, panelWidth - ENEMY_WIDTH)),
                random.nextInt(Math.max(1, 150)) + 20);
    }

    // 敌机移动逻辑（原有逻辑完全保留）
    @Override
    public void move() {
        // 逃跑优先：快速向上移出屏幕
        if (isEscaping) {
            y -= escapeSpeed;
            if (y < -ENEMY_HEIGHT) {
                hit(1); // 移出屏幕后标记为死亡
            }
            return;
        }

        // 发射子弹（按间隔随机发射，波次越高射击概率越高）
        shootBullet();
    }

    // 敌机发射子弹（原有逻辑完全保留）
    private void shootBullet() {
        long currentTime = System.currentTimeMillis();
        double shootProb = Math.min(0.1 * this.waveNumber, 0.8);
        if (currentTime - lastShootTime >= shootInterval && random.nextDouble() < shootProb) {
            // 子弹位置：敌机底部中间
            bullets.add(new EnemyBullet(x + width/2 - 3, y + height));
            lastShootTime = currentTime;
        }
    }

    // 更新敌机子弹（原有逻辑完全保留）
    public void updateBullets() {
        // 移除失效子弹
        bullets.removeIf(bullet -> !bullet.isAlive());
        // 移动子弹
        for (EnemyBullet bullet : bullets) {
            bullet.move();
        }
    }

    // 绘制敌机+子弹（核心修改：替换为图片绘制）
    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            // ========== 核心修改：删除DrawUtil绘制，改为图片绘制 ==========
            // 原代码：DrawUtil.drawEnemyAircraft((Graphics2D) g, x, y, width, height);
            Graphics2D g2d = (Graphics2D) g;
            ImageUtil.drawImage(g2d, enemyImage, x, y, ENEMY_WIDTH, ENEMY_HEIGHT);

            // 子弹绘制逻辑完全保留
            for (EnemyBullet bullet : bullets) {
                bullet.draw(g);
            }
        }
    }

    @Override
    public void die() {
        // 播放爆炸音效（原有逻辑保留）
        com.aircraftwar.util.AudioUtil.playExplodeSound();
    }

    // Getter & Setter（原有逻辑完全保留）
    public List<EnemyBullet> getBullets() { return bullets; }
    public boolean isEscaping() { return isEscaping; }
    public void setEscaping(boolean escaping) { isEscaping = escaping; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public int getWaveNumber() { return waveNumber; }
}