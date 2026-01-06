package com.aircraftwar.entity;

import com.aircraftwar.factory.ProjectileFactory;
import com.aircraftwar.entity.IBullet;
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

    // 升级相关（简易实现）
    private boolean shieldActive = false;
    private int fireLevel = 1; // lv1..lv3, 初始 lv1

    public PlayerAircraft(int x, int y) {
        super(x, y, 5, 3, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.bullets = new ArrayList<>();
        this.lastShootTime = System.currentTimeMillis();
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
            // AudioUtil.playShootSound(); // 保留音效（如有）
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

    // 新增：每帧更新，处理无敌时长结束
    public void update() {
        if (invincible && System.currentTimeMillis() > invincibleUntil) {
            invincible = false;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (isAlive()) {
            // 核心修改：绘制图片替代矩形
            Graphics2D g2d = (Graphics2D) g;
            ImageUtil.drawImage(g2d, playerImage, x, y, PLAYER_WIDTH, PLAYER_HEIGHT);

            // 若处于无敌状态，绘制金色光圈
            if (invincible) {
                // 使用半透明的金色并适当描边
                Composite oldComp = g2d.getComposite();
                Stroke oldStroke = g2d.getStroke();
                Color oldColor = g2d.getColor();

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
                g2d.setColor(new Color(255, 215, 0)); // 金色
                g2d.setStroke(new BasicStroke(4f));

                int pad = Math.max(PLAYER_WIDTH, PLAYER_HEIGHT) / 4;
                int ox = x - pad;
                int oy = y - pad;
                int ow = PLAYER_WIDTH + pad * 2;
                int oh = PLAYER_HEIGHT + pad * 2;
                g2d.drawOval(ox, oy, ow, oh);

                // 更外层的柔和光晕
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2d.setStroke(new BasicStroke(10f));
                g2d.drawOval(ox - 3, oy - 3, ow + 6, oh + 6);

                // 恢复画笔状态
                g2d.setComposite(oldComp);
                g2d.setStroke(oldStroke);
                g2d.setColor(oldColor);
            }

            // 绘制子弹（原有逻辑不变）
            for (IBullet bullet : bullets) {
                bullet.render(g);
            }
        }
    }

    /**
     * 增加火力（缩短射击间隔），有上限
     */
    public void increaseFirePower() {
        if (fireLevel >= 3) return;
        fireLevel++;
    }

    /**
     * 增加速度
     */
    public void increaseSpeed() {
        this.speed += 1; // 简单增加1
    }

    /**
     * 添加一次性盾，下一次受击消耗盾并免伤
     */
    public void addShield() {
        this.shieldActive = true;
    }

    public boolean hasShield() { return shieldActive; }

    // 覆盖 hit：在无敌期间忽略伤害，若有盾则消耗盾并忽略伤害，受伤后进入短暂无敌
    @Override
    public void hit(int damage) {
        if (invincible) return; // 无敌期间忽略伤害
        if (shieldActive) {
            // 消耗盾
            shieldActive = false;
            // 可触发盾被吸收音效
            return;
        }
        super.hit(damage);
        // 如果仍然存活则开启短暂无敌
        if (isAlive()) {
            invincible = true;
            invincibleUntil = System.currentTimeMillis() + INVINCIBLE_MS;
        }
    }

    public boolean isInvincible() {
        return invincible;
    }

    // 其他方法（moveUp/moveDown等）不变
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
