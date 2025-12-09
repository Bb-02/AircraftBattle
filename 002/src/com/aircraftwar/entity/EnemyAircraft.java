package com.aircraftwar.entity;

import com.aircraftwar.util.DrawUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 敌机类（适配小队初始位置 + 修复waveNumber未定义问题）
 */
public class EnemyAircraft extends Aircraft {
    private static final Random random = new Random();
    // 敌机尺寸
    private static final int ENEMY_WIDTH = 30;
    private static final int ENEMY_HEIGHT = 40;

    // ========== 新增：定义waveNumber成员变量 ==========
    private int waveNumber;                // 所属波次（关键修复）

    // 子弹相关
    private List<EnemyBullet> bullets;       // 敌机子弹列表
    private long lastShootTime;              // 上次发射时间
    private long shootInterval;              // 发射间隔（波次越高，间隔越短）

    // 移动相关
    private EnemyMoveType moveType;          // 移动类型
    private int moveSpeed;                   // 移动速度（波次越高越快）

    // 逃跑相关
    private boolean isEscaping;              // 是否逃跑
    private int escapeSpeed = 10;            // 逃跑速度（快速向上）

    // 构造方法支持传入初始位置（核心：初始化waveNumber）
    public EnemyAircraft(int panelWidth, int panelHeight, EnemyMoveType moveType, int waveNumber, int initX, int initY) {
        super(initX, initY, 2 + waveNumber, 1, ENEMY_WIDTH, ENEMY_HEIGHT); // 初始位置由小队指定

        // ========== 关键修复：初始化waveNumber成员变量 ==========
        this.waveNumber = waveNumber;

        // 子弹初始化
        this.bullets = new ArrayList<>();
        this.shootInterval = 3000 - (waveNumber * 200); // 波次越高，发射越快（最低800ms）
        if (shootInterval < 800) {
            shootInterval = 800;
        }
        this.lastShootTime = System.currentTimeMillis();

        // 移动初始化
        this.moveType = moveType;
        this.moveSpeed = 2 + (waveNumber / 2); // 波次越高，移动越快
        this.isEscaping = false;
    }

    // 兼容旧构造方法（避免报错）
    public EnemyAircraft(int panelWidth, int panelHeight, EnemyMoveType moveType, int waveNumber) {
        this(panelWidth, panelHeight, moveType, waveNumber,
                random.nextInt(Math.max(1, panelWidth - ENEMY_WIDTH)),
                random.nextInt(Math.max(1, 150)) + 20);
    }

    // 敌机移动逻辑（移除原位置更新，改为小队控制，仅保留射击和逃跑）
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

    // 敌机发射子弹（波次越高，射击概率越高）
    private void shootBullet() {
        long currentTime = System.currentTimeMillis();
        // ========== 此处waveNumber已定义，不再报错 ==========
        double shootProb = Math.min(0.1 * this.waveNumber, 0.8); // 波次越高，射击概率越高（最高80%）
        if (currentTime - lastShootTime >= shootInterval && random.nextDouble() < shootProb) {
            // 子弹位置：敌机底部中间
            bullets.add(new EnemyBullet(x + width/2 - 3, y + height));
            lastShootTime = currentTime;
        }
    }

    // 更新敌机子弹
    public void updateBullets() {
        // 移除失效子弹
        bullets.removeIf(bullet -> !bullet.isAlive());
        // 移动子弹
        for (EnemyBullet bullet : bullets) {
            bullet.move();
        }
    }

    // 绘制敌机+子弹
    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            DrawUtil.drawEnemyAircraft((Graphics2D) g, x, y, width, height);
            // 绘制子弹
            for (EnemyBullet bullet : bullets) {
                bullet.draw(g);
            }
        }
    }

    @Override
    public void die() {
        // 播放爆炸音效
        com.aircraftwar.util.AudioUtil.playExplodeSound();
    }

    // Getter & Setter
    public List<EnemyBullet> getBullets() { return bullets; }
    public boolean isEscaping() { return isEscaping; }
    public void setEscaping(boolean escaping) { isEscaping = escaping; }
    public void setX(int x) { this.x = x; } // 新增：支持小队修改X坐标
    public void setY(int y) { this.y = y; } // 新增：支持小队修改Y坐标
    // 新增：waveNumber的Getter（可选，便于扩展）
    public int getWaveNumber() { return waveNumber; }
}