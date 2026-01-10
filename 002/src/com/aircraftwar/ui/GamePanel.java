package com.aircraftwar.ui;

import com.aircraftwar.entity.*;
import com.aircraftwar.util.AudioUtil;
import com.aircraftwar.entity.ScoreRecord;
import com.aircraftwar.util.ScoreUtil;
import com.aircraftwar.event.EventBus;
import com.aircraftwar.event.events.FireEvent;
import com.aircraftwar.event.events.SoundEvent;
import com.aircraftwar.util.GameConfig;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
/**
 * 游戏主面板（集成雷霆战机风格小队化+无尽逻辑）
 */
public class GamePanel extends JPanel implements Runnable {
    // 游戏状态
    public static final int GAME_START = 0;
    public static final int GAME_RUNNING = 1;
    public static final int GAME_OVER = 2;
    // 新增：开始界面 -> 游戏的转场
    public static final int GAME_TRANSITION = 3;
    // 新增：游戏介绍
    public static final int GAME_HELP = 4;

    private int gameState = GAME_START;

    // 游戏元素
    private PlayerAircraft player;
    private Wave currentWave;          // 当前波次（无尽递增）
    private int currentWaveNumber = 1; // 当前波次编号
    private List<Explosion> explosions;

    // 游戏参数
    private int score = 0;

    // 键盘控制
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean shootPressed;

    // 线程控制
    private Thread gameThread;
    private boolean isRunning;
    private static final int FPS = 70; // 帧率

    // 初始化支持中文的字体（全局复用）
    private Font chineseFont;
    private Font chineseBoldFont;

    // 游戏运行时背景
    private BufferedImage backgroundImage;
    // 开始界面背景
    private BufferedImage startBackgroundImage;

    // 仅用于调试：打印一次真实面板尺寸
    private boolean printedPanelSize = false;

    // ===== 开始界面/转场效果参数 =====
    private long transitionStartMs = 0L;
    private static final long TRANSITION_MS = 520L; // 转场总时长（ms）
    private boolean transitionToGame = false;

    // ===== 受击反馈（屏幕震动/红屏） =====
    private static final int HIT_SHAKE_PX = 6;

    // ===== 难度系统（目前仅实现切换与UI展示，具体难度效果后续接入） =====
    private enum Difficulty {
        NEWBIE("新手", new Color(0, 220, 0)),
        VETERAN("老手", new Color(255, 215, 0)),
        IMPOSSIBLE("不可能", new Color(255, 60, 60));

        private final String displayName;
        private final Color color;

        Difficulty(String displayName, Color color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public Color getColor() { return color; }
    }

    // 当前难度（默认：新手）
    private Difficulty currentDifficulty = Difficulty.NEWBIE;

    // 预留扩展点：后续在 startNewWave()/EnemySquad/Wave 等处读取该值调整参数
    public String getCurrentDifficulty() {
        return currentDifficulty.getDisplayName();
    }

    // 将 Difficulty 映射为持久化用的 key（用于 ScoreUtil 分榜）
    private String getDifficultyKey() {
        switch (currentDifficulty) {
            case VETERAN:
                return "veteran";
            case IMPOSSIBLE:
                return "impossible";
            case NEWBIE:
            default:
                return "newbie";
        }
    }

    // HUD/UI 侧显示用（避免直接暴露枚举）
    private String getDifficultyDisplayName() {
        return currentDifficulty != null ? currentDifficulty.getDisplayName() : "新手";
    }

    private Color getDifficultyDisplayColor() {
        return currentDifficulty != null ? currentDifficulty.getColor() : Color.WHITE;
    }

    // 将 UI Difficulty 映射为逻辑层 DifficultyKey（entity 包内的配置层）
    private com.aircraftwar.entity.DifficultyProfile.DifficultyKey getDifficultyProfileKey() {
        switch (currentDifficulty) {
            case VETERAN:
                return com.aircraftwar.entity.DifficultyProfile.DifficultyKey.VETERAN;
            case IMPOSSIBLE:
                return com.aircraftwar.entity.DifficultyProfile.DifficultyKey.IMPOSSIBLE;
            case NEWBIE:
            default:
                return com.aircraftwar.entity.DifficultyProfile.DifficultyKey.NEWBIE;
        }
    }

    public GamePanel() {
        // 初始化面板
        // 统一屏幕尺寸到 GameConfig，避免硬编码 800/850
        setPreferredSize(new Dimension(GameConfig.SCREEN_WIDTH, GameConfig.SCREEN_HEIGHT));
        setBackground(Color.BLACK);

        // ========== 核心修复1：初始化支持中文的字体 ==========
        // 优先使用微软雅黑， fallback到宋体，确保中文显示
        chineseFont = new Font("微软雅黑", Font.PLAIN, 20);
        chineseBoldFont = new Font("微软雅黑", Font.BOLD, 25);
        // 兼容Linux/Mac系统（替换为系统自带的中文字体）
        if (chineseFont.getFamily().contains("Dialog")) {
            chineseFont = new Font("SimSun", Font.PLAIN, 20);
            chineseBoldFont = new Font("SimSun", Font.BOLD, 25);
        }

        // 加载背景图（开始界面/游戏内分别加载）
        loadBackground();
        loadStartBackground();

        // 订阅音效事件，桥接到 AudioUtil（AudioUtil 已经委托给 SoundManager）
        EventBus.getDefault().subscribe(com.aircraftwar.event.events.SoundEvent.class, (se) -> {
            try {
                String id = se.getSoundId();
                switch (id) {
                    case "shoot":
                        com.aircraftwar.util.AudioUtil.playShootSound();
                        break;
                    case "explode":
                        com.aircraftwar.util.AudioUtil.playExplodeSound();
                        break;
                    case "bgm":
                        // 约定：volume > 0 表示开始播放，<=0 停止
                        if (se.getVolume() > 0f) com.aircraftwar.util.AudioUtil.playBGM();
                        else com.aircraftwar.util.AudioUtil.stopBGM();
                        break;
                    default:
                        // 其他由 id 直接映射到 AudioUtil 或 SoundManager
                        com.aircraftwar.util.AudioUtil.playShootSound();
                        break;
                }
            } catch (Exception ignored) {}
        });

        // 订阅升级请求事件，弹出升级对话
        EventBus.getDefault().subscribe(com.aircraftwar.event.events.UpgradeRequestEvent.class, (ure) -> {
            SwingUtilities.invokeLater(() -> {
                int prevState = gameState;
                gameState = GAME_START;

                PlayerAircraft p = ure.getPlayer();
                com.aircraftwar.upgrade.UpgradeManager um = com.aircraftwar.upgrade.UpgradeManager.getInstance();

                // 全部满级：直接跳过，不弹窗
                if (!um.hasAnyAvailableUpgrade(p)) {
                    gameState = prevState;
                    return;
                }

                UpgradeDialog dlg = new UpgradeDialog(SwingUtilities.getWindowAncestor(this), p, um.getAppliedCount(p));
                int choice = dlg.showDialog();

                if (choice >= 0 && choice < com.aircraftwar.upgrade.UpgradeOption.values().length) {
                    com.aircraftwar.upgrade.UpgradeOption opt = com.aircraftwar.upgrade.UpgradeOption.values()[choice];
                    um.applyUpgrade(p, opt);
                }

                gameState = prevState;
            });
        });

        // 初始化游戏元素
        // 原先这里会直接 initGame() 并进入 GAME_RUNNING。
        // 现在改为：先进入开始界面，等待玩家按 R 开始。
        gameState = GAME_START;
        score = 0;
        currentWaveNumber = 1;
        player = new PlayerAircraft(400 - 20, 500);
        explosions = new ArrayList<>();
        currentWave = null;

        // 添加键盘监听
        addKeyListener(new GameKeyListener());
        setFocusable(true);
        // 使用更现代的 requestFocusInWindow，并在失去焦点时清理按键状态
        requestFocusInWindow();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                resetInputStates();
            }
        });

        // 启动游戏线程
        startGameThread();
    }

    // 初始化游戏
    private void initGame() {
        // 清除遗留的按键状态，确保新开局不会继承上局方向
        resetInputStates();

        // 创建玩家飞机（居中底部）
        player = new PlayerAircraft(400 - 20, 500);

        // ✅ 同步难度到升级系统，并清理该玩家的升级计数（避免串局）
        com.aircraftwar.upgrade.UpgradeManager.getInstance().setDifficulty(getDifficultyProfileKey());
        com.aircraftwar.upgrade.UpgradeManager.getInstance().resetForPlayer(player);

        // 初始化爆炸列表
        explosions = new ArrayList<>();

        // 重置游戏状态
        score = 0;
        gameState = GAME_RUNNING;
        currentWaveNumber = 1;
        // 初始化第一波（无尽型）
        startNewWave();

        // 播放背景音乐
        AudioUtil.playBGM();

        // 确保面板重新获得键盘焦点（对话框关闭或其他窗口抢占焦点后仍能输入）
        requestFocusInWindow();
    }

    // 启动新波次（无尽型，无限递增）
    private void startNewWave() {
        // 切换波次时清理按键状态，避免玩家在波次切换瞬间保持按键
        resetInputStates();

        // 绑定当前难度到波次（后续所有难度差异从这里开始传递）
        currentWave = new Wave(currentWaveNumber, getDifficultyProfileKey());

        // 调试：打印创建波次时的面板尺寸（如果为0，说明尺寸尚未布局完成）
        System.out.println("[Wave] create wave=" + currentWaveNumber + " panel=" + getWidth() + "x" + getHeight());

        // 打印波次信息（控制台）
        System.out.println("===== 无尽模式 - 第" + currentWaveNumber + "波 =====");
        System.out.println("小队数量：" + currentWave.getSquads().size());
        for (EnemySquad squad : currentWave.getSquads()) {
            System.out.println("小队" + squad.getSquadId() + "：编队=" + squad.getFormation() + "，运动=" + squad.getMoveType());
        }

        // 尝试恢复焦点，保证玩家能马上控制飞机
        requestFocusInWindow();
    }

    // 检查波次切换（无尽型，波次无限递增）
    private void checkWaveSwitch() {
        if (currentWave.isWaveOver()) {
            // 波次结束，启动下一波（无尽递增）
            currentWaveNumber++;
            startNewWave();
        }
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
                } else if (gameState == GAME_TRANSITION) {
                    updateTransition();
                }
                delta--;
            }

            // 处理事件总线中队列的事件（在主线程/主循环处理，避免异步订阅者并发问题）
            EventBus.getDefault().drain();

            repaint(); // 重绘界面

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 转场更新：时间到后真正进入/重开游戏
    private void updateTransition() {
        long now = System.currentTimeMillis();
        if (transitionStartMs <= 0L) transitionStartMs = now;

        long elapsed = now - transitionStartMs;
        if (elapsed >= TRANSITION_MS) {
            // 转场结束：真正开始游戏
            if (transitionToGame) {
                // 清理标记，避免递归
                transitionToGame = false;
                transitionStartMs = 0L;
                initGame();
            } else {
                // 预留：未来可做“返回主菜单”等
                gameState = GAME_START;
                transitionStartMs = 0L;
            }
        }
    }

    // 开始一次“进入游戏”的转场
    private void startTransitionToGame() {
        resetInputStates();
        transitionToGame = true;
        transitionStartMs = System.currentTimeMillis();
        gameState = GAME_TRANSITION;
        requestFocusInWindow();
    }

    // 更新游戏逻辑
    private void updateGame() {
        if (!printedPanelSize) {
            printedPanelSize = true;
            System.out.println("[GamePanel] size=" + getWidth() + "x" + getHeight() + " preferred=" + getPreferredSize());
        }

        // currentWave/player 为空时直接跳过（开始界面/转场阶段不会走到这里，但加一层保险更稳）
        if (player == null || currentWave == null) return;

        // 控制玩家移动
        controlPlayerMovement();


        // 更新当前波次（小队移动+子弹)
        currentWave.updateWave();

        // 更新玩家状态（新增：处理无敌计时）
        player.update();

        // 更新玩家子弹
        if (shootPressed) {
            player.shoot();
            EventBus.getDefault().post(new FireEvent(0, "basic", player.getX(), player.getY()));
            EventBus.getDefault().post(new SoundEvent("shoot", 1.0f));
        }
        player.updateBullets();

        // 碰撞检测
        checkCollisions();

        // 更新爆炸效果
        updateExplosions();

        // 检查波次切换（无尽型）
        checkWaveSwitch();

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

    // 碰撞检测（适配小队敌机）
    private void checkCollisions() {
        List<EnemyAircraft> allEnemies = currentWave.getAllEnemies();

        // 1. 玩家子弹击中敌机
        for (EnemyAircraft enemy : allEnemies) {
            if (!enemy.isAlive()) continue;

            Iterator<IBullet> bulletIterator = player.getBullets().iterator();
            while (bulletIterator.hasNext()) {
                IBullet bullet = bulletIterator.next();
                if (bullet.isAlive() && bullet.getCollisionRect().intersects(enemy.getCollisionRect())) {
                    // apply damage from bullet to enemy
                    enemy.hit(bullet.getDamage());
                    bullet.setAlive(false);
                    bulletIterator.remove();
                    // 添加爆炸效果
                    explosions.add(new Explosion(enemy.getX(), enemy.getY()));
                    // 发布爆炸音效事件和得分事件
                    EventBus.getDefault().post(new SoundEvent("explode", 1.0f));
                    int oldScore = score;
                    score += 10 * currentWaveNumber; // 波次越高，得分越高（无尽难度奖励）
                    EventBus.getDefault().post(new com.aircraftwar.event.events.ScoreChangedEvent(oldScore, score, player));
                    break;
                }
            }
        }

        // 2. 敌机子弹击中玩家
        for (EnemyAircraft enemy : allEnemies) {
            if (!enemy.isAlive()) continue;

            Iterator<IBullet> enemyBulletIterator = enemy.getBullets().iterator();
            while (enemyBulletIterator.hasNext()) {
                IBullet eBullet = enemyBulletIterator.next();
                if (eBullet.isAlive() && eBullet.getCollisionRect().intersects(player.getCollisionRect())) {
                    // 玩家扣血：固定每次 1 点，避免出现“单次命中扣 2HP”
                    player.hit(1);
                    eBullet.setAlive(false);
                    enemyBulletIterator.remove();
                    // 添加爆炸效果
                    explosions.add(new Explosion(player.getX(), player.getY()));
                    EventBus.getDefault().post(new SoundEvent("explode", 1.0f));

                    // 关键：受击后立刻无敌，本帧不再继续处理更多子弹/碰撞，防止“堆子弹秒杀”
                    return;
                }
            }
        }

        // 3. 敌机碰撞玩家
        for (EnemyAircraft enemy : allEnemies) {
            if (enemy.isAlive() && enemy.getCollisionRect().intersects(player.getCollisionRect())) {
                player.hit(1); // 只扣1血，和敌机子弹一致
                enemy.hit(1);
                explosions.add(new Explosion(player.getX(), player.getY()));
                EventBus.getDefault().post(new SoundEvent("explode", 1.0f));

                // 同理：撞击后本帧也停止后续碰撞结算
                return;
            }
        }
    }

    // 检查游戏结束
    // GamePanel.java 中 checkGameOver() 方法的修改
    // ========== 额外优化：确保JOptionPane输入框支持中文输入 ==========
    private void checkGameOver() {
        if (!player.isAlive() && gameState != GAME_OVER) {
            gameState = GAME_OVER;
            resetInputStates();
            AudioUtil.stopBGM();
            AudioUtil.playGameOverSound();

            SwingUtilities.invokeLater(() -> {
                // 1. 初始化对话框
                JDialog inputDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "输入昵称", true);
                inputDialog.setSize(650, 315); // 加宽高度，确保所有组件有足够空间
                inputDialog.setLocationRelativeTo(this);
                inputDialog.setLayout(new BorderLayout(10, 15)); // 上下间距
                inputDialog.getContentPane().setBackground(Color.WHITE);
                inputDialog.setResizable(false);


                // 2. 提示文本区域（垂直排列，确保每行独立）
                JPanel tipPanel = new JPanel();
                tipPanel.setLayout(new BoxLayout(tipPanel, BoxLayout.Y_AXIS));
                tipPanel.setBackground(Color.WHITE);
                tipPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10)); // 内边距

                // 2.1 游戏结束提示（加粗、红色）
                JLabel gameOverLabel = new JLabel("游戏结束！");
                gameOverLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
                gameOverLabel.setForeground(Color.RED);
                gameOverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                tipPanel.add(gameOverLabel);

                // 2.2 得分提示
                JLabel scoreLabel = new JLabel("你的得分：" + score);
                scoreLabel.setFont(chineseFont);
                scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                tipPanel.add(Box.createVerticalStrut(8));
                tipPanel.add(scoreLabel);

                // 2.3 波次提示
                JLabel waveLabel = new JLabel("到达波次：" + currentWaveNumber);
                waveLabel.setFont(chineseFont);
                waveLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                tipPanel.add(Box.createVerticalStrut(8));
                tipPanel.add(waveLabel);

                // 2.4 输入提示
                JLabel inputTipLabel = new JLabel("请输入昵称（最多8个中文/16个英文）：");
                inputTipLabel.setFont(chineseFont);
                inputTipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                tipPanel.add(Box.createVerticalStrut(12));
                tipPanel.add(inputTipLabel);

                inputDialog.add(tipPanel, BorderLayout.NORTH);


                // 3. 输入框+计数标签区域（水平排列，确保都显示）
                JPanel inputAreaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // 水平居中排列
                inputAreaPanel.setBackground(Color.WHITE);
                inputAreaPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

                // 3.1 输入框（设置最小宽度，避免被挤压）
                JTextField nicknameField = new JTextField();
                nicknameField.setFont(chineseFont);
                nicknameField.setPreferredSize(new Dimension(250, 30)); // 固定宽度高度
                nicknameField.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                // 3.2 计数标签
                JLabel countLabel = new JLabel("剩余可输入：16 字符");
                countLabel.setFont(chineseFont);
                countLabel.setForeground(Color.GRAY);

                inputAreaPanel.add(nicknameField);
                inputAreaPanel.add(countLabel);
                inputDialog.add(inputAreaPanel, BorderLayout.CENTER);


                // 4. 字符长度监听（逻辑不变）
                nicknameField.getDocument().addDocumentListener(new DocumentListener() {
                    private int calculateCharLength(String text) {
                        int length = 0;
                        for (char c : text.toCharArray()) {
                            length += (c >= 0x4E00 && c <= 0x9FA5) ? 2 : 1;
                        }
                        return length;
                    }

                    private String truncateText(String text) {
                        StringBuilder sb = new StringBuilder();
                        int currentLength = 0;
                        for (char c : text.toCharArray()) {
                            int charLen = (c >= 0x4E00 && c <= 0x9FA5) ? 2 : 1;
                            if (currentLength + charLen > 16) break;
                            sb.append(c);
                            currentLength += charLen;
                        }
                        return sb.toString();
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) { updateCount(); }
                    @Override
                    public void removeUpdate(DocumentEvent e) { updateCount(); }
                    @Override
                    public void changedUpdate(DocumentEvent e) { updateCount(); }

                    private void updateCount() {
                        String text = nicknameField.getText();
                        if (calculateCharLength(text) > 16) {
                            nicknameField.setText(truncateText(text));
                            text = nicknameField.getText();
                        }
                        int used = calculateCharLength(text);
                        int remaining = 16 - used;
                        countLabel.setText("剩余可输入：" + remaining + " 字符");
                        countLabel.setForeground(remaining < 0 ? Color.RED : Color.GRAY);
                    }
                });


                // 5. 确认按钮区域（居中）
                JPanel btnPanel = new JPanel();
                btnPanel.setBackground(Color.WHITE);
                JButton confirmBtn = new JButton("确认");
                confirmBtn.setFont(chineseFont);
                confirmBtn.setPreferredSize(new Dimension(100, 35));
                confirmBtn.setBackground(Color.LIGHT_GRAY);
                confirmBtn.setBorderPainted(false);

                String[] nicknameHolder = new String[1];
                confirmBtn.addActionListener(e -> {
                    nicknameHolder[0] = nicknameField.getText().trim();
                    inputDialog.dispose();
                });
                btnPanel.add(confirmBtn);
                inputDialog.add(btnPanel, BorderLayout.SOUTH);


                // 6. 关闭对话框默认值
                inputDialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        nicknameHolder[0] = null;
                    }
                });


                // 显示对话框
                inputDialog.setVisible(true);
                String nickname = nicknameHolder[0];

                // 按当前难度保存到对应排行榜
                ScoreUtil.saveScore(getDifficultyKey(), nickname, score);

                repaint();
            });
        }
    }

    // 清理所有按键状态（在新局/波次开始或失去焦点时调用）
    private void resetInputStates() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        shootPressed = false;
    }
    // 在 GamePanel 类内，添加字段并在构造器中加载图片
    public void loadBackground() {
        // 统一走 ImageUtil（文件系统优先，classpath 兜底）
        backgroundImage = com.aircraftwar.util.ImageUtil.loadImage("Background.png");
    }

    // 开始界面背景
    private void loadStartBackground() {
        // 统一走 ImageUtil（文件系统优先，classpath 兜底）
        startBackgroundImage = com.aircraftwar.util.ImageUtil.loadImage("Background2.png");
    }

    // 绘制游戏界面（新增小队/波次信息）
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // ===== 受击反馈：屏幕轻微震动（只在游戏运行中） =====
        int shakeX = 0;
        int shakeY = 0;
        if (gameState == GAME_RUNNING && player != null && player.isRecentlyHit()) {
            shakeX = (int) (Math.random() * (HIT_SHAKE_PX * 2 + 1)) - HIT_SHAKE_PX;
            shakeY = (int) (Math.random() * (HIT_SHAKE_PX * 2 + 1)) - HIT_SHAKE_PX;
            g2d.translate(shakeX, shakeY);
        }

        if (!printedPanelSize) {
            printedPanelSize = true;
            Insets in = getInsets();
            System.out.println("[GamePanel] size=" + getWidth() + "x" + getHeight() + " insets=" + in);
        }

        // 开启文字抗锯齿，避免中文显示模糊
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // 背景：开始界面/转场/介绍界面统一用 Background2
        boolean isStartLike = (gameState == GAME_START || gameState == GAME_TRANSITION || gameState == GAME_HELP);
        BufferedImage bg = isStartLike ? startBackgroundImage : backgroundImage;

        if (bg != null) {
            // 简单拉伸铺满整个面板：
            g2d.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
        } else {
            // 图片加载失败，使用纯色背景作为兜底
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }


        if (gameState == GAME_START || gameState == GAME_TRANSITION) {
            // ===== 开始界面（含转场覆盖层） =====
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font titleFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 56f) : new Font("微软雅黑", Font.BOLD, 56);
            Font menuFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 26f) : new Font("微软雅黑", Font.BOLD, 26);
            Font tipFont = chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 20f) : new Font("微软雅黑", Font.PLAIN, 20);

            String title = "飞机大战";
            String line1 = "R 开始游戏";
            String line2 = "T 游戏难度：" + currentDifficulty.getDisplayName();
            String line3 = "Y 游戏介绍";
            String bottom = "WASD 控制飞机移动，空格发射子弹";

            // 标题
            g2d.setFont(titleFont);
            FontMetrics fmTitle = g2d.getFontMetrics();
            int titleX = (getWidth() - fmTitle.stringWidth(title)) / 2;
            int titleY = getHeight() / 2 - 180;
            g2d.setColor(Color.WHITE);
            g2d.drawString(title, titleX, titleY);

            // 菜单
            g2d.setFont(menuFont);
            FontMetrics fmMenu = g2d.getFontMetrics();
            int startY = titleY + 90;
            int gap = 46;

            // 1) R 的闪烁高亮（只在开始界面/转场前半段显示更明显）
            boolean blinkOn = (System.currentTimeMillis() / 380) % 2 == 0;
            if (gameState == GAME_START && blinkOn) {
                g2d.setColor(new Color(255, 215, 0));
            } else {
                g2d.setColor(Color.WHITE);
            }
            int x1 = (getWidth() - fmMenu.stringWidth(line1)) / 2;
            g2d.drawString(line1, x1, startY);

            // 第二行：难度（按难度上色）
            g2d.setColor(currentDifficulty.getColor());
            int x2 = (getWidth() - fmMenu.stringWidth(line2)) / 2;
            g2d.drawString(line2, x2, startY + gap);

            // 第三行：Y
            g2d.setColor(Color.WHITE);
            int x3 = (getWidth() - fmMenu.stringWidth(line3)) / 2;
            g2d.drawString(line3, x3, startY + gap * 2);

            // 底部说明
            g2d.setFont(tipFont);
            FontMetrics fmTip = g2d.getFontMetrics();
            int bottomX = (getWidth() - fmTip.stringWidth(bottom)) / 2;
            int bottomY = getHeight() - 40;
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.drawString(bottom, bottomX, bottomY);

            // 2) 转场覆盖层：淡出开始界面（黑色蒙版 alpha 从 0->1）
            if (gameState == GAME_TRANSITION) {
                long now = System.currentTimeMillis();
                double t = (transitionStartMs <= 0L) ? 0.0 : Math.min(1.0, (now - transitionStartMs) / (double) TRANSITION_MS);
                // 前半程淡出（0->1），后半程保持黑屏（由 initGame 后自然进入 GAME_RUNNING）
                float alpha = (float) Math.min(1.0, t * 1.25);

                Composite old = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setComposite(old);
            }

            // 开始界面/转场绘制完毕
            if (gameState != GAME_RUNNING) return;
        }

        if (gameState == GAME_HELP) {
            // ===== 游戏介绍界面 =====
            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font titleFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 46f) : new Font("微软雅黑", Font.BOLD, 46);
            Font sectionFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 28f) : new Font("微软雅黑", Font.BOLD, 28);
            Font itemFont = chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 22f) : new Font("微软雅黑", Font.PLAIN, 22);
            Font tipFont = chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 18f) : new Font("微软雅黑", Font.PLAIN, 18);

            String title = "游戏介绍";
            g2d.setFont(titleFont);
            FontMetrics fmTitle = g2d.getFontMetrics();
            int titleX = (getWidth() - fmTitle.stringWidth(title)) / 2;
            int titleY = 90;
            g2d.setColor(Color.WHITE);
            g2d.drawString(title, titleX, titleY);

            // 难度介绍
            g2d.setFont(sectionFont);
            g2d.setColor(Color.WHITE);
            String section = "难度介绍";
            int secX = (getWidth() - g2d.getFontMetrics().stringWidth(section)) / 2;
            int y = titleY + 80;
            g2d.drawString(section, secX, y);

            y += 40;

            // 三档难度词条
            int leftX = 110;
            int rightX = getWidth() / 2 + 40;
            int colY = y + 30;

            // 新手
            g2d.setFont(sectionFont);
            g2d.setColor(Difficulty.NEWBIE.getColor());
            g2d.drawString("新手", leftX, colY);
            g2d.setFont(itemFont);
            g2d.setColor(Color.WHITE);
            int iy = colY + 34;
            g2d.drawString("• 敌人轻微攻击欲望", leftX, iy);
            g2d.drawString("• 正常敌机密度", leftX, iy + 30);
            g2d.drawString("• 正常敌人频率", leftX, iy + 60);

            // 老手
            g2d.setFont(sectionFont);
            g2d.setColor(Difficulty.VETERAN.getColor());
            g2d.drawString("老手", rightX, colY);
            g2d.setFont(itemFont);
            g2d.setColor(Color.WHITE);
            int jy = colY + 34;
            g2d.drawString("• 敌人正常攻击欲望", rightX, jy);
            g2d.drawString("• 敌人出现频率增加", rightX, jy + 30);

            // 不可能
            int thirdY = colY + 160;
            g2d.setFont(sectionFont);
            g2d.setColor(Difficulty.IMPOSSIBLE.getColor());
            g2d.drawString("不可能", leftX, thirdY);
            g2d.setFont(itemFont);
            g2d.setColor(Color.WHITE);
            int ky = thirdY + 34;
            g2d.drawString("• 敌人强攻击欲望", leftX, ky);
            g2d.drawString("• 弹幕压力大", leftX, ky + 30);
            g2d.drawString("• 升级压力大", leftX, ky + 60);

            // 底部提示
            g2d.setFont(tipFont);
            String back = "按 Q 或 ESC 返回开始界面";
            int backX = (getWidth() - g2d.getFontMetrics().stringWidth(back)) / 2;
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.drawString(back, backX, getHeight() - 40);

            return;
        }

        if (gameState == GAME_RUNNING) {
            // 绘制玩家
            player.draw(g);

            // 绘制当前波所有小队的敌机
            List<EnemySquad> squads = currentWave.getSquads();
            for (EnemySquad squad : squads) {
                if (squad.isSpawned()) {
                    for (EnemyAircraft enemy : squad.getEnemies()) {
                        enemy.draw(g);
                    }
                }
            }

            // 绘制独立敌人（Bee 等不属于小队的敌人）
            // 注意：Bee 已在 Wave.getAllEnemies() 中参与碰撞，这里只负责渲染
            for (BeeAircraft bee : currentWave.getIndependentBees()) {
                if (bee != null) {
                    // Bee.draw() 已实现：机体只在存活时绘制，但子弹不依赖存活
                    bee.draw(g);
                }
            }

            // 绘制爆炸效果
            for (Explosion explosion : explosions) {
                explosion.draw(g);
            }

            // 绘制游戏信息（雷霆战机风格UI）

            g.setColor(Color.WHITE);
            // 使用中文字体变量并设置大小与样式（比直接 new Font 更可靠）
            Font infoFont = chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 20f) : new Font("微软雅黑", Font.PLAIN, 20);
            g.setFont(infoFont);

            // 分数（波次越高，得分倍率越高）
            g.drawString("分数: " + score + " (x" + currentWaveNumber + ")", 20, 30);
            // 历史最高分（按难度）
            g.drawString("历史最高分: " + ScoreUtil.getHighestScore(getDifficultyKey()), 20, 60);
            // 当前波次
            g.drawString("当前波次: " + currentWaveNumber, 20, 90);

            // 游戏难度（颜色跟开始界面一致）
            String difficultyText = getDifficultyDisplayName();
            Color difficultyColor = getDifficultyDisplayColor();
            g.setColor(difficultyColor != null ? difficultyColor : Color.WHITE);
            g.drawString("游戏难度: " + difficultyText, 20, 120);

            // 画完难度后恢复默认颜色，避免影响后续 UI
            g.setColor(Color.WHITE);

            // 玩家生命值（使用加粗中文字体）
            Font hpFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 20f) : new Font("微软雅黑", Font.BOLD, 20);
            g.setFont(hpFont);

            // 受击：HP 文字短暂变红+抖动
            int hpDx = 0;
            int hpDy = 0;
            if (player != null && player.isRecentlyHit()) {
                g.setColor(new Color(255, 90, 90));
                hpDx = (int) (Math.random() * 5) - 2;
                hpDy = (int) (Math.random() * 5) - 2;
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString("HP: " + player.getHp(), getWidth() - 80 + hpDx, 30 + hpDy);

            // 受击红屏遮罩（短暂）
            if (player != null && player.isRecentlyHit()) {
                Composite old = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
                g2d.setColor(new Color(255, 0, 0));
                // 注意：此时可能已 translate，直接画满面板即可
                g2d.fillRect(-HIT_SHAKE_PX, -HIT_SHAKE_PX, getWidth() + HIT_SHAKE_PX * 2, getHeight() + HIT_SHAKE_PX * 2);
                g2d.setComposite(old);
            }

        } else if (gameState == GAME_OVER) {
            // ===== 排行榜/结算界面：统一居中排版 =====
            int centerX = getWidth() / 2;
            int baseY = getHeight() / 2 - 180;

            // 1) GAME OVER
            Font overFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 56f) : new Font("微软雅黑", Font.BOLD, 56);
            g2d.setFont(overFont);
            g2d.setColor(new Color(255, 80, 80));
            String overText = "GAME OVER";
            int overX = centerX - g2d.getFontMetrics().stringWidth(overText) / 2;
            g2d.drawString(overText, overX, baseY);

            // 2) 结算信息
            Font infoFont = chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 26f) : new Font("微软雅黑", Font.PLAIN, 26);
            g2d.setFont(infoFont);
            g2d.setColor(Color.WHITE);

            String s1 = "最终得分: " + score;
            String s2 = "到达波次: " + currentWaveNumber;
            String s3 = "历史最高分: " + ScoreUtil.getHighestScore(getDifficultyKey());

            int y = baseY + 70;
            g2d.drawString(s1, centerX - g2d.getFontMetrics().stringWidth(s1) / 2, y);
            y += 40;
            g2d.drawString(s2, centerX - g2d.getFontMetrics().stringWidth(s2) / 2, y);
            y += 40;
            g2d.drawString(s3, centerX - g2d.getFontMetrics().stringWidth(s3) / 2, y);

            // 3) 排行榜标题："排行榜 Top 5 - "(白) + 难度名(按难度上色)
            Font titleFont = chineseBoldFont != null ? chineseBoldFont.deriveFont(Font.BOLD, 28f) : new Font("微软雅黑", Font.BOLD, 28);
            g2d.setFont(titleFont);

            String titlePrefix = "排行榜 Top 5 - ";
            String diffName = currentDifficulty.getDisplayName();

            FontMetrics fmTitle = g2d.getFontMetrics();
            int prefixW = fmTitle.stringWidth(titlePrefix);
            int diffW = fmTitle.stringWidth(diffName);
            int titleW = prefixW + diffW;
            int titleX = centerX - titleW / 2;

            y += 70;
            g2d.setColor(Color.WHITE);
            g2d.drawString(titlePrefix, titleX, y);
            g2d.setColor(currentDifficulty.getColor());
            g2d.drawString(diffName, titleX + prefixW, y);

            // 4) 排行榜列表
            g2d.setFont(chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 22f) : new Font("微软雅黑", Font.PLAIN, 22));
            g2d.setColor(Color.WHITE);
            List<ScoreRecord> topRecords = ScoreUtil.getTopScores(getDifficultyKey());

            int listY = y + 45;
            int lineGap = 32;
            int max = Math.min(topRecords.size(), 5);
            if (max == 0) {
                String empty = "暂无记录";
                g2d.drawString(empty, centerX - g2d.getFontMetrics().stringWidth(empty) / 2, listY);
                listY += lineGap;
            } else {
                for (int i = 0; i < max; i++) {
                    ScoreRecord record = topRecords.get(i);
                    String rankText = String.format("%d. %s  -  %d", i + 1, record.getNickname(), record.getScore());
                    int x = centerX - g2d.getFontMetrics().stringWidth(rankText) / 2;
                    g2d.drawString(rankText, x, listY);
                    listY += lineGap;
                }
            }

            // 5) 操作提示
            g2d.setFont(chineseFont != null ? chineseFont.deriveFont(Font.PLAIN, 20f) : new Font("微软雅黑", Font.PLAIN, 20));
            g2d.setColor(new Color(255, 255, 255, 230));

            String restartText = "按 R 键开始新游戏";
            String backText = "按 Q 返回开始界面";

            int rx = centerX - g2d.getFontMetrics().stringWidth(restartText) / 2;
            int bx = centerX - g2d.getFontMetrics().stringWidth(backText) / 2;

            g2d.drawString(restartText, rx, listY + 10);
            g2d.drawString(backText, bx, listY + 10 + 30);
        }

        // 还原 translate，避免影响 Swing 后续绘制
        if (shakeX != 0 || shakeY != 0) {
            g2d.translate(-shakeX, -shakeY);
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
                    // 开始界面：按 R 进入转场；结束界面：按 R 重新开始（也走转场）
                    if (gameState == GAME_START || gameState == GAME_OVER) {
                        startTransitionToGame();
                    }
                    break;
                case KeyEvent.VK_T:
                    // 仅在开始界面允许切换难度（避免游戏中误触）
                    if (gameState == GAME_START) {
                        switch (currentDifficulty) {
                            case NEWBIE:
                                currentDifficulty = Difficulty.VETERAN;
                                break;
                            case VETERAN:
                                currentDifficulty = Difficulty.IMPOSSIBLE;
                                break;
                            case IMPOSSIBLE:
                            default:
                                currentDifficulty = Difficulty.NEWBIE;
                                break;
                        }
                        repaint();
                    }
                    break;
                case KeyEvent.VK_Y:
                    // 开始界面：进入游戏介绍
                    if (gameState == GAME_START) {
                        resetInputStates();
                        gameState = GAME_HELP;
                        repaint();
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    // 介绍界面：返回
                    if (gameState == GAME_HELP) {
                        resetInputStates();
                        gameState = GAME_START;
                        repaint();
                    }
                    break;
                case KeyEvent.VK_Q:
                    // 介绍界面：返回开始界面
                    if (gameState == GAME_HELP) {
                        resetInputStates();
                        gameState = GAME_START;
                        AudioUtil.stopBGM();
                        repaint();
                        break;
                    }

                    // 游戏结束界面：返回开始界面
                    if (gameState == GAME_OVER) {
                        resetInputStates();
                        gameState = GAME_START;
                        // 回到开始界面不播放 BGM（你若希望菜单也有BGM可后续加）
                        AudioUtil.stopBGM();
                        repaint();
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
