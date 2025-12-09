package com.aircraftwar.ui;

import com.aircraftwar.entity.Aircraft;
import com.aircraftwar.entity.EnemyAircraft;
import com.aircraftwar.entity.Explosion;
import com.aircraftwar.entity.PlayerAircraft;
import com.aircraftwar.util.AudioUtil;
import com.aircraftwar.util.ScoreRecord;
import com.aircraftwar.util.ScoreUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 游戏主面板（集成昵称输入+昵称+分数排行榜）
 */
public class GamePanel extends JPanel implements Runnable {
    // 游戏状态
    public static final int GAME_START = 0;
    public static final int GAME_RUNNING = 1;
    public static final int GAME_OVER = 2;

    private int gameState = GAME_START;

    // 游戏元素
    private PlayerAircraft player;
    private List<EnemyAircraft> enemies;
    private List<Explosion> explosions;

    // 游戏参数
    private int score = 0;
    private int enemySpawnInterval = 1000; // 敌机生成间隔（毫秒）
    private long lastEnemySpawnTime;
    private Random random = new Random();

    // 键盘控制
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean shootPressed;

    // 线程控制
    private Thread gameThread;
    private boolean isRunning;
    private static final int FPS = 60; // 帧率

    public GamePanel() {
        // 初始化面板
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);

        // 初始化游戏元素
        initGame();

        // 添加键盘监听
        addKeyListener(new GameKeyListener());
        setFocusable(true);

        // 启动游戏线程
        startGameThread();
    }

    // 初始化游戏
    private void initGame() {
        // 创建玩家飞机（居中底部）
        player = new PlayerAircraft(400 - 20, 500);

        // 初始化集合
        enemies = new ArrayList<>();
        explosions = new ArrayList<>();

        // 重置游戏状态
        score = 0;
        gameState = GAME_RUNNING;
        lastEnemySpawnTime = System.currentTimeMillis();

        // 播放背景音乐
        AudioUtil.playBGM();
    }

    // 启动游戏线程
    private void startGameThread() {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // 游戏主循环
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerFrame = 1_000_000_000.0 / FPS;
        double delta = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerFrame;
            lastTime = now;

            while (delta >= 1) {
                if (gameState == GAME_RUNNING) {
                    updateGame(); // 更新游戏逻辑
                }
                delta--;
            }

            repaint(); // 重绘界面

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 更新游戏逻辑
    private void updateGame() {
        // 控制玩家移动
        controlPlayerMovement();

        // 生成敌机
        spawnEnemies();

        // 更新敌机
        updateEnemies();

        // 更新玩家子弹
        if (shootPressed) {
            player.shoot();
        }
        player.updateBullets();

        // 碰撞检测
        checkCollisions();

        // 更新爆炸效果
        updateExplosions();

        // 检查游戏结束
        checkGameOver();
    }

    // 控制玩家移动
    private void controlPlayerMovement() {
        if (upPressed) player.moveUp();
        if (downPressed) player.moveDown(getHeight());
        if (leftPressed) player.moveLeft();
        if (rightPressed) player.moveRight(getWidth());
    }

    // 生成敌机
    private void spawnEnemies() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEnemySpawnTime >= enemySpawnInterval) {
            enemies.add(new EnemyAircraft(getWidth(), getHeight()));
            lastEnemySpawnTime = currentTime;

            // 逐渐提高难度
            if (enemySpawnInterval > 300) {
                enemySpawnInterval -= 10;
            }
        }
    }

    // 更新敌机
    private void updateEnemies() {
        Iterator<EnemyAircraft> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            EnemyAircraft enemy = iterator.next();
            enemy.move();

            // 移除飞出屏幕或已死亡的敌机
            if (enemy.isOutOfScreen(getHeight()) || !enemy.isAlive()) {
                if (!enemy.isAlive()) {
                    // 添加爆炸效果
                    explosions.add(new Explosion(enemy.getX(), enemy.getY()));
                    score += 10; // 加分
                }
                iterator.remove();
            }
        }
    }

    // 更新爆炸效果
    private void updateExplosions() {
        Iterator<Explosion> iterator = explosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            if (explosion.isExpired()) {
                iterator.remove();
            }
        }
    }

    // 碰撞检测
    private void checkCollisions() {
        // 子弹击中敌机
        for (EnemyAircraft enemy : enemies) {
            if (!enemy.isAlive()) continue;

            Iterator<com.aircraftwar.entity.Bullet> bulletIterator = player.getBullets().iterator();
            while (bulletIterator.hasNext()) {
                com.aircraftwar.entity.Bullet bullet = bulletIterator.next();
                if (bullet.isAlive() &&
                        bullet.getCollisionRect().intersects(enemy.getCollisionRect())) {
                    bullet.hitTarget(enemy);
                    bulletIterator.remove();
                    break;
                }
            }
        }

        // 敌机碰撞玩家
        for (EnemyAircraft enemy : enemies) {
            if (enemy.isAlive() &&
                    enemy.getCollisionRect().intersects(player.getCollisionRect())) {
                player.hit(1);
                enemy.hit(1);
                explosions.add(new Explosion(player.getX(), player.getY()));
                break;
            }
        }
    }

    // 检查游戏结束（新增昵称输入逻辑）
    private void checkGameOver() {
        if (!player.isAlive() && gameState != GAME_OVER) {
            gameState = GAME_OVER;
            AudioUtil.stopBGM();
            AudioUtil.playGameOverSound();

            // 在Swing事件线程中弹出昵称输入框（避免线程问题）
            SwingUtilities.invokeLater(() -> {
                // 弹出输入对话框，提示玩家输入昵称
                String nickname = JOptionPane.showInputDialog(
                        this,
                        "Game Over!\nYour Score: " + score + "\nPlease enter your nickname:",
                        "Enter Nickname",
                        JOptionPane.PLAIN_MESSAGE
                );
                // 保存昵称+分数到排行榜
                ScoreUtil.saveScore(nickname, score);
                // 重绘界面（显示带昵称的排行榜）
                repaint();
            });
        }
    }

    // 绘制游戏界面（优化：显示昵称+分数排行榜）
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gameState == GAME_RUNNING) {
            // 绘制玩家
            player.draw(g);

            // 绘制玩家子弹
            player.drawBullets(g);

            // 绘制敌机
            for (EnemyAircraft enemy : enemies) {
                enemy.draw(g);
            }

            // 绘制爆炸效果
            for (Explosion explosion : explosions) {
                explosion.draw(g);
            }

            // 绘制分数
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Score: " + score, 20, 30);
            // 绘制历史最高分
            g.drawString("Highest: " + ScoreUtil.getHighestScore(), 20, 60);

            // 绘制生命值
            g.drawString("HP: " + player.getHp(), getWidth() - 80, 30);
        } else if (gameState == GAME_OVER) {
            // 绘制游戏结束界面
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("GAME OVER", getWidth()/2 - 150, getHeight()/2 - 180);

            // 绘制当前分数和历史最高分
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString("Final Score: " + score, getWidth()/2 - 100, getHeight()/2 - 100);
            g.drawString("Highest Score: " + ScoreUtil.getHighestScore(), getWidth()/2 - 120, getHeight()/2 - 50);

            // 绘制排行榜标题
            g.setFont(new Font("Arial", Font.BOLD, 25));
            g.drawString("Top 10 Players", getWidth()/2 - 80, getHeight()/2);

            // 绘制排行榜列表（昵称 + 分数）
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            List<ScoreRecord> topRecords = ScoreUtil.getScoreRecordList();
            int yOffset = 0;
            for (int i = 0; i < topRecords.size(); i++) {
                yOffset += 30;
                ScoreRecord record = topRecords.get(i);
                // 格式化显示：排名. 昵称 - 分数
                String rankText = String.format("%d. %s - %d",
                        i+1, record.getNickname(), record.getScore());
                // 居中对齐（调整x坐标适配不同长度的昵称）
                int textX = getWidth()/2 - (rankText.length() * 5);
                g.drawString(rankText, textX, getHeight()/2 + yOffset);
            }

            // 绘制重启提示
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("Press R to Restart", getWidth()/2 - 80, getHeight()/2 + yOffset + 50);
        }
    }

    // 键盘监听器
    private class GameKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();

            switch (keyCode) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    upPressed = true;
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    downPressed = true;
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    leftPressed = true;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    rightPressed = true;
                    break;
                case KeyEvent.VK_SPACE:
                    shootPressed = true;
                    break;
                case KeyEvent.VK_R:
                    // 重新开始游戏
                    if (gameState == GAME_OVER) {
                        initGame();
                    }
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int keyCode = e.getKeyCode();

            switch (keyCode) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    upPressed = false;
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    downPressed = false;
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    leftPressed = false;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    rightPressed = false;
                    break;
                case KeyEvent.VK_SPACE:
                    shootPressed = false;
                    break;
            }
        }
    }

    // 停止游戏
    public void stopGame() {
        isRunning = false;
        AudioUtil.stopBGM();
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}