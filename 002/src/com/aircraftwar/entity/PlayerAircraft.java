package com.aircraftwar.entity;

import com.aircraftwar.factory.ProjectileFactory;
import com.aircraftwar.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayerAircraft extends Aircraft {
    private List<IBullet> bullets;
    private long lastShootTime;
    private long shootInterval = 200;

    // 玩家飞机尺寸（和图片适配）
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 50;
    // 玩家飞机图片
    private BufferedImage playerImage;

    // 新增：无敌相关状态（受伤后短暂无敌）
    private boolean invincible = false;
    private long invincibleUntil = 0L;
    private static final long INVINCIBLE_MS = 1000L; // 1秒无敌

    // 升级相关：弹道等级 lv1..lv3, 初始 lv1
    private int fireLevel = 1;
    private static final int MAX_FIRE_LEVEL = 3;

    // 升级相关：移速等级 lv0..lv3（初始lv0，每升一级更快）
    private int speedLevel = 0;
    private final int baseSpeed;
    private static final int MAX_SPEED_LEVEL = 3;
    private static final int SPEED_PER_LEVEL = 1; // 每级增加的速度（可调）

    public PlayerAircraft(int x, int y) {
        super(x, y, 5, 3, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.bullets = new ArrayList<>();
        this.lastShootTime = System.currentTimeMillis();
        this.baseSpeed = this.speed;

        // 加载玩家飞机图片 + 日志
        System.out.println("[PlayerAircraft] 开始加载 PlayerPlane.png");
        this.playerImage = ImageUtil.loadImage("PlayerPlane.png");
        if (this.playerImage == null) {
            System.out.println("[PlayerAircraft] ❌ PlayerPlane.png 加载失败！");
        } else {
            System.out.println("[PlayerAircraft] ✅ PlayerPlane.png 加载成功！");
        }
    }

    @Override
    public void move() {
        // 边界检查兜底
        x = Math.max(0, Math.min(800 - PLAYER_WIDTH, x));
        y = Math.max(0, Math.min(600 - PLAYER_HEIGHT, y));
    }

    public void shoot() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastShootTime >= shootInterval) {
            // 根据当前火力等级发射多条弹道（同一行，左右分布）
            int n = Math.max(1, Math.min(3, fireLevel));
            int spacing = 16; // 子弹间距（像素）
            for (int i = 0; i < n; i++) {
                int shift = (2 * i - (n - 1)) * spacing / 2; // 对称分布
                int fakePlayerX = x + shift;
                IBullet b = ProjectileFactory.createPlayerBullet(fakePlayerX, y, PLAYER_WIDTH);
                bullets.add(b);
            }
            lastShootTime = currentTime;
        }
    }

    public void updateBullets() {
        Iterator<IBullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            IBullet bullet = iterator.next();
            bullet.update();
            if (!bullet.isAlive()) {
                iterator.remove();
            }
        }
    }

    // 每帧更新：处理无敌时长结束
    public void update() {
        if (invincible && System.currentTimeMillis() > invincibleUntil) {
            invincible = false;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (!isAlive()) return;

        Graphics2D g2d = (Graphics2D) g;
        ImageUtil.drawImage(g2d, playerImage, x, y, PLAYER_WIDTH, PLAYER_HEIGHT);

        // 无敌期间渲染金色光圈
        if (invincible) {
            Composite oldComp = g2d.getComposite();
            Stroke oldStroke = g2d.getStroke();
            Color oldColor = g2d.getColor();

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(4f));

            int pad = Math.max(PLAYER_WIDTH, PLAYER_HEIGHT) / 4;
            int ox = x - pad;
            int oy = y - pad;
            int ow = PLAYER_WIDTH + pad * 2;
            int oh = PLAYER_HEIGHT + pad * 2;
            g2d.drawOval(ox, oy, ow, oh);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2d.setStroke(new BasicStroke(10f));
            g2d.drawOval(ox - 3, oy - 3, ow + 6, oh + 6);

            g2d.setComposite(oldComp);
            g2d.setStroke(oldStroke);
            g2d.setColor(oldColor);
        }

        // 绘制子弹
        for (IBullet bullet : bullets) {
            bullet.render(g);
        }
    }

    /**
     * 增加火力等级：lv1 -> lv2(2发) -> lv3(3发)
     */
    public void increaseFirePower() {
        if (fireLevel >= MAX_FIRE_LEVEL) return;
        fireLevel++;
    }

    public int getFireLevel() {
        return fireLevel;
    }

    public int getMaxFireLevel() {
        return MAX_FIRE_LEVEL;
    }

    /**
     * 增加速度：lv0 -> lv1 -> lv2 -> lv3
     */
    public void increaseSpeed() {
        if (speedLevel >= MAX_SPEED_LEVEL) return;
        speedLevel++;
        this.speed = baseSpeed + speedLevel * SPEED_PER_LEVEL;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public int getMaxSpeedLevel() {
        return MAX_SPEED_LEVEL;
    }

    @Override
    public void hit(int damage) {
        // 已经无敌：直接忽略
        if (invincible) return;

        // 关键：先立刻进入无敌，再处理扣血。
        // 这样同一帧里如果又发生多次碰撞，后续 hit() 会被立即拦截。
        invincible = true;
        invincibleUntil = System.currentTimeMillis() + INVINCIBLE_MS;

        super.hit(damage);

        // 如果被打死了，无敌也就无所谓了；保持字段即可
        if (!isAlive()) {
            // 可选：这里不需要额外处理
        }
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void moveUp() {
        y -= speed;
        if (y < 0) y = 0;
    }

    public void moveDown(int panelHeight) {
        y += speed;
        if (y > panelHeight - height) y = panelHeight - height;
    }

    public void moveLeft() {
        x -= speed;
        if (x < 0) x = 0;
    }

    public void moveRight(int panelWidth) {
        x += speed;
        if (x > panelWidth - width) x = panelWidth - width;
    }

    @Override
    public void die() {
        // AudioUtil.playPlayerDeadSound(); // 保留音效（如有）
    }

    public List<IBullet> getBullets() {
        return bullets;
    }
}
