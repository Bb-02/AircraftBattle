// language: java
package Plane_Game_2025;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 游戏主面板，负责游戏逻辑更新和界面绘制
 */
public class GamePanel extends JPanel implements Runnable {
    // 游戏常量
    public static final int SCREEN_WIDTH = 800;
    public static final int SCREEN_HEIGHT = 600;
    private static final int FPS = 60; // 帧率
    private static final long FRAME_INTERVAL = 1000 / FPS; // 每帧间隔时间

    // 游戏状态枚举
    private enum GameState {
        READY, RUNNING, GAME_OVER
    }

    // 游戏状态
    private GameState gameState = GameState.READY;

    // 游戏实体
    private PlayerPlane player; // 玩家飞机
    private final List<EnemyPlane> enemyPlanes = new ArrayList<>(); // 敌人飞机
    private final List<Bullet> playerBullets = new ArrayList<>(); // 玩家子弹
    private final List<Bullet> enemyBullets = new ArrayList<>(); // 敌人子弹
    private final List<Explosion> explosions = new ArrayList<>(); // 爆炸效果

    // 游戏数据
    private int killCount = 0; // 击杀数
    private long startTime; // 游戏开始时间
    private long surviveTime; // 存活时间（秒）
    private int score = 0; // 总分

    // 系统变量
    private final Random random = new Random();
    private Thread gameThread; // 游戏主线程
    private Timer enemySpawnTimer; // 敌人生成定时器
    private Timer enemyShootTimer; // 敌人射击定时器

    // 难度配置
    private static final int INIT_ENEMY_SPAWN_INTERVAL = 1500; // 初始敌人生成间隔（毫秒）
    private static final int MIN_ENEMY_SPAWN_INTERVAL = 400; // 最小生成间隔
    private static final int DIFFICULTY_UPGRADE_CYCLE = 8000; // 难度提升周期（毫秒）
    private static final int SPAWN_INTERVAL_DECREASE = 80; // 每次难度提升减少的间隔
    private long lastDifficultyUpgradeTime; // 上次难度提升时间

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setDoubleBuffered(true); // 双缓冲，防止绘制闪烁
        setFocusable(true); // 获取焦点，接收键盘事件

        // 初始化玩家飞机（屏幕中央）
        initPlayer();

        // 初始化定时器
        initTimers();

        // 注册事件监听
        registerListeners();
    }

    /**
     * 初始化玩家飞机
     */
    private void initPlayer() {
        int playerX = SCREEN_WIDTH / 2 - PlayerPlane.WIDTH / 2;
        int playerY = SCREEN_HEIGHT / 2 - PlayerPlane.HEIGHT / 2;
        player = new PlayerPlane(playerX, playerY);
    }

    /**
     * 初始化定时器
     */
    private void initTimers() {
        // 敌人生成定时器
        enemySpawnTimer = new Timer(INIT_ENEMY_SPAWN_INTERVAL, e -> {
            if (gameState == GameState.RUNNING) {
                spawnEnemy();
            }
        });

        // 敌人射击定时器（1.2秒/次）
        enemyShootTimer = new Timer(1200, e -> {
            if (gameState == GameState.RUNNING) {
                enemyPlanes.forEach(enemy -> {
                    if (enemy.isAlive()) {
                        Bullet bullet = enemy.shoot();
                        if (bullet != null) {
                            enemyBullets.add(bullet);
                        }
                    }
                });
            }
        });
    }

    /**
     * 注册鼠标和键盘监听
     */
    private void registerListeners() {
        // 鼠标移动控制玩家飞机
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameState == GameState.RUNNING && player.isAlive()) {
                    // 鼠标坐标转换为飞机中心坐标
                    int targetX = e.getX() - PlayerPlane.WIDTH / 2;
                    int targetY = e.getY() - PlayerPlane.HEIGHT / 2;
                    player.setTargetPosition(targetX, targetY);
                }
            }
        });

        // 键盘事件（开始游戏/重新开始）
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (gameState == GameState.READY) {
                        startGame(); // 准备状态 → 开始游戏
                    } else if (gameState == GameState.GAME_OVER) {
                        restartGame(); // 游戏结束 → 重新开始
                    }
                }
            }
        });
    }

    /**
     * 开始游戏
     */
    public void startGame() {
        gameState = GameState.RUNNING;
        startTime = System.currentTimeMillis();
        lastDifficultyUpgradeTime = startTime;

        // 启动定时器
        enemySpawnTimer.start();
        enemyShootTimer.start();

        // 启动游戏线程
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    /**
     * 重新开始游戏
     */
    private void restartGame() {
        // 重置游戏状态和数据
        gameState = GameState.RUNNING;
        killCount = 0;
        surviveTime = 0;
        score = 0;

        // 清空所有实体
        enemyPlanes.clear();
        playerBullets.clear();
        enemyBullets.clear();
        explosions.clear();

        // 重新初始化玩家
        initPlayer();

        // 重置难度和定时器
        enemySpawnTimer.setDelay(INIT_ENEMY_SPAWN_INTERVAL);
        lastDifficultyUpgradeTime = System.currentTimeMillis();

        // 重启定时器
        enemySpawnTimer.restart();
        enemyShootTimer.restart();
    }

    /**
     * 生成敌人飞机（从屏幕上方随机位置出现）
     */
    private void spawnEnemy() {
        int enemyWidth = EnemyPlane.WIDTH;
        int enemyHeight = EnemyPlane.HEIGHT;
        // 随机X坐标（确保敌人完全在屏幕内）
        int x = random.nextInt(SCREEN_WIDTH - enemyWidth);
        int y = -enemyHeight; // 初始位置在屏幕上方外，平滑进入
        enemyPlanes.add(new EnemyPlane(x, y));
    }

    /**
     * 游戏逻辑更新（每帧执行）
     */
    private void update() {
        if (gameState != GameState.RUNNING) return;

        // 更新存活时间
        surviveTime = (System.currentTimeMillis() - startTime) / 1000;

        // 难度提升（每8秒减少敌人生成间隔）
        updateDifficulty();

        // 更新玩家飞机（移动+自动射击）
        if (player.isAlive()) {
            player.move(); // 平滑移动
            // 自动射击（200ms/发）
            if (System.currentTimeMillis() - player.getLastShootTime() > 200) {
                Bullet bullet = player.shoot();
                playerBullets.add(bullet);
                player.setLastShootTime(System.currentTimeMillis());
            }
        }

        // 更新敌人飞机（移动）
        for (EnemyPlane enemy : enemyPlanes) {
            if (enemy.isAlive()) {
                // 朝向玩家中心移动
                enemy.move(player.getX() + PlayerPlane.WIDTH / 2, player.getY() + PlayerPlane.HEIGHT / 2);
                // 敌人出界（屏幕下方）则标记死亡
                if (enemy.getY() > SCREEN_HEIGHT) {
                    enemy.setAlive(false);
                }
            }
        }

        // 更新子弹（移动+出界检测）
        updateBullets();

        // 更新爆炸效果
        updateExplosions();

        // 碰撞检测（子弹vs飞机）
        checkCollisions();

        // 清理死亡实体（提升性能）
        cleanUpDeadEntities();

        // 计算分数（存活时间×10 + 击杀数×100）
        score = (int) surviveTime * 10 + killCount * 100;
    }

    /**
     * 更新难度（敌人生成间隔递减）
     */
    private void updateDifficulty() {
        if (System.currentTimeMillis() - lastDifficultyUpgradeTime > DIFFICULTY_UPGRADE_CYCLE) {
            int currentInterval = enemySpawnTimer.getDelay();
            if (currentInterval > MIN_ENEMY_SPAWN_INTERVAL) {
                currentInterval -= SPAWN_INTERVAL_DECREASE;
                enemySpawnTimer.setDelay(currentInterval);
            }
            lastDifficultyUpgradeTime = System.currentTimeMillis();
        }
    }

    /**
     * 更新子弹状态
     */
    private void updateBullets() {
        // 玩家子弹
        for (Bullet bullet : playerBullets) {
            if (bullet.isAlive()) {
                bullet.move();
                // 出界则标记死亡
                if (bullet.getX() < 0 || bullet.getX() > SCREEN_WIDTH ||
                        bullet.getY() < 0 || bullet.getY() > SCREEN_HEIGHT) {
                    bullet.setAlive(false);
                }
            }
        }

        // 敌人子弹
        for (Bullet bullet : enemyBullets) {
            if (bullet.isAlive()) {
                bullet.move();
                if (bullet.getX() < 0 || bullet.getX() > SCREEN_WIDTH ||
                        bullet.getY() < 0 || bullet.getY() > SCREEN_HEIGHT) {
                    bullet.setAlive(false);
                }
            }
        }
    }

    /**
     * 更新爆炸效果（生命周期管理）
     */
    private void updateExplosions() {
        Iterator<Explosion> iterator = explosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.update();
            if (!explosion.isAlive()) {
                iterator.remove(); // 爆炸结束则移除
            }
        }
    }

    /**
     * 碰撞检测
     */
    private void checkCollisions() {
        // 1. 玩家子弹 vs 敌人飞机
        Iterator<Bullet> bulletIt = playerBullets.iterator();
        while (bulletIt.hasNext()) {
            Bullet bullet = bulletIt.next();
            if (!bullet.isAlive()) {
                bulletIt.remove();
                continue;
            }

            Iterator<EnemyPlane> enemyIt = enemyPlanes.iterator();
            while (enemyIt.hasNext()) {
                EnemyPlane enemy = enemyIt.next();
                if (enemy.isAlive() && bullet.getBounds().intersects(enemy.getBounds())) {
                    // 碰撞：子弹和敌人都死亡，添加爆炸效果
                    bullet.setAlive(false);
                    enemy.setAlive(false);
                    explosions.add(new Explosion(enemy.getX() + enemy.getWidth()/2, enemy.getY() + enemy.getHeight()/2));
                    killCount++; // 击杀数+1
                    bulletIt.remove();
                    enemyIt.remove();
                    break;
                }
            }
        }

        // 2. 敌人子弹 vs 玩家飞机
        for (Bullet bullet : enemyBullets) {
            if (bullet.isAlive() && player.isAlive() && bullet.getBounds().intersects(player.getBounds())) {
                // 碰撞：玩家和子弹死亡，添加爆炸效果
                player.setAlive(false);
                bullet.setAlive(false);
                explosions.add(new Explosion(player.getX() + PlayerPlane.WIDTH/2, player.getY() + PlayerPlane.HEIGHT/2));
                gameOver(); // 游戏结束
                break;
            }
        }

        // 3. 玩家飞机边界检测（超出边界直接爆炸）
        if (player.isAlive()) {
            if (player.getX() < 0 || player.getX() > SCREEN_WIDTH - PlayerPlane.WIDTH ||
                    player.getY() < 0 || player.getY() > SCREEN_HEIGHT - PlayerPlane.HEIGHT) {
                player.setAlive(false);
                explosions.add(new Explosion(player.getX() + PlayerPlane.WIDTH/2, player.getY() + PlayerPlane.HEIGHT/2));
                gameOver();
            }
        }
    }

    /**
     * 清理死亡实体（释放内存）
     */
    private void cleanUpDeadEntities() {
        // 清理敌人飞机
        enemyPlanes.removeIf(enemy -> !enemy.isAlive());
        // 清理玩家子弹
        playerBullets.removeIf(bullet -> !bullet.isAlive());
        // 清理敌人子弹
        enemyBullets.removeIf(bullet -> !bullet.isAlive());
    }

    /**
     * 游戏结束
     */
    private void gameOver() {
        gameState = GameState.GAME_OVER;
        // 停止所有定时器
        enemySpawnTimer.stop();
        enemyShootTimer.stop();
    }

    /**
     * 绘制游戏界面
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // 抗锯齿渲染，让图形更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制背景（深黑色，护眼）
        g2d.setColor(new Color(5, 5, 15));
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // 根据游戏状态绘制不同内容
        switch (gameState) {
            case READY:
                drawReadyScreen(g2d);
                break;
            case RUNNING:
                drawGameScreen(g2d);
                break;
            case GAME_OVER:
                drawGameOverScreen(g2d);
                break;
        }
    }

    /**
     * 绘制准备界面
     */
    private void drawReadyScreen(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        // 标题
        Font titleFont = new Font("微软雅黑", Font.BOLD, 48);
        g2d.setFont(titleFont);
        String title = "飞机大战";
        int titleWidth = g2d.getFontMetrics().stringWidth(title);
        g2d.drawString(title, (SCREEN_WIDTH - titleWidth) / 2, SCREEN_HEIGHT / 2 - 60);

        // 提示文字
        Font hintFont = new Font("微软雅黑", Font.PLAIN, 24);
        g2d.setFont(hintFont);
        String hint1 = "鼠标移动控制飞机  自动发射子弹";
        String hint2 = "按空格键开始游戏";
        int hint1Width = g2d.getFontMetrics().stringWidth(hint1);
        int hint2Width = g2d.getFontMetrics().stringWidth(hint2);
        g2d.drawString(hint1, (SCREEN_WIDTH - hint1Width) / 2, SCREEN_HEIGHT / 2);
        g2d.drawString(hint2, (SCREEN_WIDTH - hint2Width) / 2, SCREEN_HEIGHT / 2 + 50);
    }

    /**
     * 绘制游戏运行界面
     */
    private void drawGameScreen(Graphics2D g2d) {
        // 绘制玩家飞机
        if (player.isAlive()) {
            player.draw(g2d);
        }

        // 绘制敌人飞机
        for (EnemyPlane enemy : enemyPlanes) {
            if (enemy.isAlive()) {
                enemy.draw(g2d);
            }
        }

        // 绘制玩家子弹
        for (Bullet bullet : playerBullets) {
            if (bullet.isAlive()) {
                bullet.draw(g2d);
            }
        }

        // 绘制敌人子弹
        for (Bullet bullet : enemyBullets) {
            if (bullet.isAlive()) {
                bullet.draw(g2d);
            }
        }

        // 绘制爆炸效果
        for (Explosion explosion : explosions) {
            explosion.draw(g2d);
        }

        // 绘制游戏信息（击杀数、存活时间、分数）
        drawGameInfo(g2d);
    }

    /**
     * 绘制游戏结束界面
     */
    private void drawGameOverScreen(Graphics2D g2d) {
        // 绘制半透明遮罩
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // 游戏结束标题
        g2d.setColor(Color.RED);
        Font titleFont = new Font("微软雅黑", Font.BOLD, 52);
        g2d.setFont(titleFont);
        String gameOverText = "游戏结束";
        int titleWidth = g2d.getFontMetrics().stringWidth(gameOverText);
        g2d.drawString(gameOverText, (SCREEN_WIDTH - titleWidth) / 2, SCREEN_HEIGHT / 2 - 80);

        // 游戏数据统计
        g2d.setColor(Color.WHITE);
        Font dataFont = new Font("微软雅黑", Font.PLAIN, 28);
        g2d.setFont(dataFont);
        String killText = "击杀数: " + killCount;
        String timeText = "存活时间: " + surviveTime + " 秒";
        String scoreText = "最终分数: " + score;
        int killWidth = g2d.getFontMetrics().stringWidth(killText);
        int timeWidth = g2d.getFontMetrics().stringWidth(timeText);
        int scoreWidth = g2d.getFontMetrics().stringWidth(scoreText);

        g2d.drawString(killText, (SCREEN_WIDTH - killWidth) / 2, SCREEN_HEIGHT / 2 - 20);
        g2d.drawString(timeText, (SCREEN_WIDTH - timeWidth) / 2, SCREEN_HEIGHT / 2 + 30);
        g2d.drawString(scoreText, (SCREEN_WIDTH - scoreWidth) / 2, SCREEN_HEIGHT / 2 + 80);

        // 重新开始提示
        Font restartFont = new Font("微软雅黑", Font.PLAIN, 24);
        g2d.setFont(restartFont);
        String restartText = "按空格键重新开始";
        int restartWidth = g2d.getFontMetrics().stringWidth(restartText);
        g2d.drawString(restartText, (SCREEN_WIDTH - restartWidth) / 2, SCREEN_HEIGHT / 2 + 140);
    }

    /**
     * 绘制游戏信息（顶部状态栏）
     */
    private void drawGameInfo(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        Font infoFont = new Font("微软雅黑", Font.PLAIN, 18);
        g2d.setFont(infoFont);
        String info = String.format("击杀数: %d  |  存活时间: %ds  |  分数: %d", killCount, surviveTime, score);
        g2d.drawString(info, 20, 30);
    }

    /**
     * 游戏主线程（控制帧率）- 修复重新开始功能
     */
    @Override
    public void run() {
        long lastFrameTime = System.currentTimeMillis();
        // 线程持续运行，不退出（通过游戏状态控制更新逻辑）
        while (true) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastFrameTime;

            // 达到帧间隔时间则更新和绘制
            if (elapsedTime >= FRAME_INTERVAL) {
                // 只有运行状态才更新游戏逻辑，其他状态只绘制界面
                if (gameState == GameState.RUNNING) {
                    update(); // 更新游戏逻辑
                }
                repaint(); // 重绘界面（准备/结束状态也需要绘制）
                lastFrameTime = currentTime;
            } else {
                // 未达到间隔，短暂休眠，避免CPU占用过高
                try {
                    Thread.sleep(FRAME_INTERVAL - elapsedTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}