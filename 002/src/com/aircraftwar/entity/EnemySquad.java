package com.aircraftwar.entity;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 敌机小队类（参考雷霆战机编队逻辑，改为分阶段智能行为）
 */
public class EnemySquad {
    // 编队形状枚举
    public enum Formation {
        HORIZONTAL, // 横排（左右排列）
        VERTICAL,   // 竖排（上下排列）
        TRIANGLE,   // 三角编队
        DIAMOND     // 菱形编队
    }

    // 阶段化行为：进入/巡航/攻击/重组/撤退
    private enum Phase {
        ENTER, PATROL, ATTACK, REGROUP, EXIT
    }

    private int squadId;                // 小队编号
    private Formation formation;        // 编队形状
    private EnemyMoveType moveType;     // 小队运动类型（用于基础风格）
    private int enemyCount;             // 小队敌机数量
    private int waveNumber;             // 所属波次
    private List<EnemyAircraft> enemies;// 小队敌机列表
    // 新增：标记敌机对象是否已真正创建（lazy spawn）
    private boolean enemiesCreated = false;

    private long spawnDelay;            // 生成延迟（波内小队的生成间隔）
    private int baseX, baseY;           // 小队基准位置（编队中心，用于渲染/外部读取）
    private double baseXf, baseYf;      // 新增：小队基准位置（double 缓冲，消除抖动）
    private int targetBaseY;            // 进入阶段目标Y
    private double targetBaseYf;        // 新增：进入阶段目标Y（double 缓冲）
    private int moveSpeed;              // 小队移动速度（随波次递增）
    private boolean isSpawned;          // 是否已生成
    private boolean isAllDead;          // 小队是否全灭

    // 编队槽位偏移（每架机相对 baseX/baseY 的偏移）
    private final List<Point> formationOffsets = new ArrayList<>();

    // 每个小队的相位偏移：让 sin/cos 轨迹在不同小队间错开，避免“同步抽搐”
    private final double phaseOffset = random.nextDouble() * Math.PI * 2;

    // 运动参数（适配不同轨迹）
    // private int moveDirection = 1;      // 已废弃：改用 velX/velY 驱动

    private static final Random random = new Random();

    // 新增：分阶段状态与每架敌机俯冲状态
    private Phase phase = Phase.ENTER;
    private long phaseStartTime;
    private Map<EnemyAircraft, DiveInfo> diveMap = new HashMap<>();
    private final int panelWidth = 800;
    private final int panelHeight = 850;
    private final int boundary = 50;

    // 玩家位置（由外部设置，用于俯冲目标）
    private volatile int playerX = panelWidth / 2;
    private volatile int playerY = panelHeight - 100;

    // =====================
    // 删除：脱队/归队（个别机独立机动）
    // =====================
    // private static final long BREAKOFF_MIN_INTERVAL_MS = 1800;
    // private static final long BREAKOFF_MAX_INTERVAL_MS = 4200;
    // private static final long BREAKOFF_DURATION_MS = 1200;
    // private static final int BREAKOFF_MAX_ACTIVE = 2; // 同时最多脱队数量（避免太乱）

    // private long nextBreakoffTime = 0L;

    // private final Map<EnemyAircraft, BreakoffInfo> breakoffMap = new HashMap<>();

    // 新增：普通阶段敌机活动的最低高度（y 越大越靠近底部）；避免玩家打不到
    private static final int COMBAT_MAX_Y = 700;

    // 方案B：俯冲允许更低一点，但仍然不贴底，确保玩家基本还能打到
    private static final int DIVE_MAX_Y = 760;

    // =====================
    // 反抖动：基准位速度向量 + 反弹冷却
    // =====================
    private double velX = 0.0;
    private double velY = 0.0;
    private long lastBounceTime = 0L;
    private static final long BOUNCE_COOLDOWN_MS = 90; // 反弹后短暂冷却，避免边界来回翻

    // 基准速度的平滑控制
    private double targetVelX = 0.0;
    private double targetVelY = 0.0;
    private static final double VEL_SMOOTH = 0.10; // 越大越“跟手”，越小越平滑

    // ZIGZAG/PATROL 等阶段的目标高度（避免写死 300/350 引发钉住抖动）
    private double roamMinY = 70;
    private double roamMaxY = 320;

    // 新增：入场时在屏幕外的安全偏移，确保整队（含编队偏移）都在画面外
    // 进一步加大，按“约 3 架敌机长度”推到更外侧，避免肉眼仍觉得在画面内生成
    private static final int SPAWN_OUTSIDE_PADDING_Y = 120 + 44 * 3;

    // 可调：生成点距离屏幕顶端（y=0）多远（以敌机高度为单位）
    private static final int SPAWN_OUTSIDE_ENEMY_HEIGHTS = 3; // 更近一点：约 3 架敌机高度

    // 调试开关：用于定位 spawn 是否仍在画面内（提交前可改回 false）
    private static final boolean DEBUG_SPAWN = false;

    // 入场最短时长：避免刚 spawn 下一帧就被推进到 y>=0，看起来像“突然生成在画面里”
    private static final long ENTER_MIN_TIME_MS = 450;

    // 入场采用“俯冲同款”插值曲线：从屏幕外冲入到目标位
    private double enterProgress = 0.0;
    private double enterProgressStep = 0.020; // 每帧推进，越小越慢越丝滑
    private double enterStartXf = 0.0;
    private double enterStartYf = 0.0;
    // 新增：锁定入场目标点（避免入场期间目标点变化导致最后一帧跳变）
    private double enterTargetXf = 0.0;
    private double enterTargetYf = 0.0;

    // 入场曲线参数：更像“冲入→急刹车”
    //（已弃用：现在使用 enterProgress 插值冲入）
    // private static final long ENTER_RUSH_MS = 420;
    // private static final long ENTER_BRAKE_MS = 520;
    // private static final double ENTER_RUSH_MAX_VY = 10.0;
    // private static final double ENTER_BRAKE_MIN_VY = 0.55;

    // 入场“丝滑急刹”参数：不回弹，临界阻尼收敛
    //（已弃用：现在使用 enterProgress 插值冲入）
    // private static final double ENTER_APPROACH_ZONE = 90.0;
    // private static final double ENTER_STOP_EPS = 1.6;
    // private static final double ENTER_STOP_VE = 0.55;
    // private static final double ENTER_SMOOTH = 0.22;
    // private static final double ENTER_MAX_STEP = 12.0;

    // 新增：入场弧线幅度（像俯冲一样轻微弯一下）
    private static final double ENTER_CURVE_AMPLITUDE = 10.0;

    // 入场目标高度范围（更自然：进入后停在屏幕上方一段距离，再开始巡航）
    private static final int ENTER_TARGET_Y_MIN = 50;
    private static final int ENTER_TARGET_Y_MAX = 120;

    // 构造方法（根据波次生成小队配置，难度递增）
    public EnemySquad(int squadId, int waveNumber, long spawnDelay) {
        this.squadId = squadId;
        this.waveNumber = waveNumber;
        this.spawnDelay = spawnDelay;
        this.isSpawned = false;
        this.isAllDead = false;

        // 波次越高，小队敌机数越多（1-5架）
        this.enemyCount = 1 + (waveNumber / 2);
        if (enemyCount > 5) enemyCount = 5;

        // 波次越高，移动速度越快
        this.moveSpeed = 4 + waveNumber;
        if (moveSpeed > 8) moveSpeed = 8;

        // 随机选择编队形状和运动类型（波次越高，复杂运动类型概率越高）
        double complexProb = Math.min(0.1 * waveNumber, 0.9);
        if (random.nextDouble() < complexProb) {
            // 移除 SPIRAL：它容易产生“原地转圈圈”的呆板感
            int typeIdx = random.nextInt(2);
            switch (typeIdx) {
                case 0: this.moveType = EnemyMoveType.DIAGONAL_BACK_FORTH; break;
                case 1: this.moveType = EnemyMoveType.ZIGZAG; break;
                default: this.moveType = EnemyMoveType.SWAY_FORWARD; break;
            }
        } else {
            this.moveType = random.nextBoolean() ? EnemyMoveType.LEFT_RIGHT : EnemyMoveType.HOVER;
        }

        int formationIdx = random.nextInt(4);
        switch (formationIdx) {
            case 0: this.formation = Formation.HORIZONTAL; break;
            case 1: this.formation = Formation.VERTICAL; break;
            case 2: this.formation = Formation.TRIANGLE; break;
            default: this.formation = Formation.DIAMOND; break;
        }

        // 初始基准位置从屏幕上方外侧进入
        this.baseX = random.nextInt(600) + 100; // X: 100-700

        // 关键修复：让“最下面那架(最大offsetY)”也在屏幕外
        // 最大编队纵向偏移：enemyCount<=5 时约为 3*spacingY=108，保守取 140
        int maxFormationOffsetY = 140;
        this.baseY = -SPAWN_OUTSIDE_PADDING_Y - maxFormationOffsetY - random.nextInt(200);

        // 入场目标位置（屏幕上半区偏下，避免太靠前）
        this.targetBaseY = ENTER_TARGET_Y_MIN + random.nextInt(ENTER_TARGET_Y_MAX - ENTER_TARGET_Y_MIN + 1);

        // 同步 double 缓冲
        this.baseXf = this.baseX;
        this.baseYf = this.baseY;
        this.targetBaseYf = this.targetBaseY;

        // 这里不再提前创建敌机对象（避免尚未 spawn 就被渲染/被移动导致“在画面里生成”）
        this.enemies = new ArrayList<>();
        this.enemiesCreated = false;

        // 仍然提前生成编队偏移（用于 spawn 时计算最大偏移，保证严格屏幕外）
        generateFormationOffsetsOnly();

        // 俯冲状态表等在真正创建敌机后再初始化
        diveMap.clear();

        this.phase = Phase.ENTER;
        this.phaseStartTime = System.currentTimeMillis();

        // 初始化速度向量：给每队一个不同的初始方向，避免同步抽搐
        double a = random.nextDouble() * Math.PI * 2;
        double baseSp = Math.max(1.2, moveSpeed * 0.55);
        this.velX = Math.cos(a) * baseSp;
        this.velY = Math.sin(a) * (baseSp * 0.35);
    }

    // 仅生成编队槽位偏移，不创建敌机对象
    private void generateFormationOffsetsOnly() {
        int spacingX = 46;
        int spacingY = 36;
        formationOffsets.clear();

        Point[] stagger = {
                new Point(0, 0),
                new Point(-spacingX / 2, spacingY),
                new Point(spacingX / 2, spacingY),
                new Point(-spacingX / 2, 2 * spacingY),
                new Point(spacingX / 2, 2 * spacingY)
        };

        switch (formation) {
            case VERTICAL:
                for (int i = 0; i < enemyCount; i++) {
                    int ox = (i % 2 == 0) ? 0 : spacingX / 3;
                    int oy = i * spacingY;
                    formationOffsets.add(new Point(ox, oy));
                }
                break;
            case TRIANGLE:
                Point[] tri = {
                        new Point(0, 0),
                        new Point(-spacingX / 2, spacingY),
                        new Point(spacingX / 2, spacingY),
                        new Point(0, 2 * spacingY),
                        new Point(0, 3 * spacingY)
                };
                for (int i = 0; i < enemyCount; i++) {
                    formationOffsets.add(new Point(tri[i]));
                }
                break;
            case HORIZONTAL:
            case DIAMOND:
            default:
                for (int i = 0; i < enemyCount; i++) {
                    formationOffsets.add(new Point(stagger[i]));
                }
                break;
        }
    }

    // 在 spawn 时才真正创建敌机对象
    private void createEnemiesAtCurrentBase() {
        if (enemiesCreated) return;
        enemiesCreated = true;

        enemies.clear();
        for (Point off : formationOffsets) {
            int x = baseX + off.x;
            int y = baseY + off.y;
            addEnemy(x, y);
        }

        // 初始化每架敌机的俯冲状态
        diveMap.clear();
        for (EnemyAircraft e : enemies) {
            diveMap.put(e, new DiveInfo());
        }
    }

    // 按编队形状生成小队敌机（同时记录初始偏移）
    // （已弃用：改为 lazy spawn，在 checkSpawn() 内 createEnemiesAtCurrentBase()）
    @Deprecated
    private void generateSquadEnemies() {
        // no-op
    }

    // 添加单架敌机到小队
    private void addEnemy(int x, int y) {
        // Jellyfish 概率：波次越高出现概率越大（上限 35%）
        double jellyProb = Math.min(0.10 + waveNumber * 0.03, 0.35);

        EnemyAircraft enemy;
        if (random.nextDouble() < jellyProb) {
            enemy = new JellyfishAircraft(waveNumber, x, y);
        } else {
            enemy = new EnemyAircraft(
                    800, 850, moveType, waveNumber, x, y // 修复：panelHeight 应与游戏面板一致
            );
        }
        enemies.add(enemy);
    }

    // 检查小队是否到生成时间
    public void checkSpawn(long waveStartTime) {
        if (!isSpawned && System.currentTimeMillis() - waveStartTime >= spawnDelay) {
            isSpawned = true;
            phase = Phase.ENTER;
            phaseStartTime = System.currentTimeMillis();

            // 每次真正 spawn 时重新随机一个 X
            this.baseX = random.nextInt(600) + 100;
            this.baseXf = this.baseX;

            // 先计算严格屏幕外的 baseY
            int maxOffsetY = getFormationMaxOffsetY();
            int enemyH = 44;
            int minOutsidePx = enemyH * SPAWN_OUTSIDE_ENEMY_HEIGHTS;
            int extraRandom = random.nextInt(enemyH * 3);
            this.baseYf = -(minOutsidePx + enemyH + maxOffsetY + extraRandom);
            this.baseY = (int) this.baseYf;

            // 入场目标位置（更自然）
            this.targetBaseY = ENTER_TARGET_Y_MIN + random.nextInt(ENTER_TARGET_Y_MAX - ENTER_TARGET_Y_MIN + 1);
            this.targetBaseYf = this.targetBaseY;

            // 每次 spawn 重置入场后缓冲
            postEnterHoldUntil = 0L;

            // ENTER 阶段速度清零（ENTER 只由插值驱动）
            this.velX = 0.0;
            this.velY = 0.0;
            this.targetVelX = 0.0;
            this.targetVelY = 0.0;

            // ✅ 关键：现在才真正创建敌机对象（否则 enemies 为空，看不到敌机）
            this.enemiesCreated = false;
            createEnemiesAtCurrentBase();

            // 初始化入场插值起点（当前屏幕外位置）
            enterStartXf = baseXf;
            enterStartYf = baseYf;
            enterProgress = 0.0;
            enterProgressStep = 0.018 + random.nextDouble() * 0.010;

            // ✅ 锁定入场目标点：必须在 targetBaseYf 同步后
            enterTargetXf = this.baseXf;
            enterTargetYf = this.targetBaseYf;


            // spawn 当帧：把每架敌机坐标同步到“屏幕外”位置
            for (int slotIndex = 0; slotIndex < enemies.size(); slotIndex++) {
                EnemyAircraft enemy = enemies.get(slotIndex);
                if (enemy == null) continue;
                Point baseOffset = slotIndex < formationOffsets.size() ? formationOffsets.get(slotIndex) : new Point(0, 0);

                int sx = (int) Math.round(baseXf + baseOffset.x);
                int sy = (int) Math.round(baseYf + baseOffset.y);
                enemy.setX(sx);
                enemy.setY(sy);

                if (DEBUG_SPAWN) {
                    System.out.println("[EnemySquad][spawn] squad=" + squadId + " enemy=" + slotIndex + " xy=(" + sx + "," + sy + ")");
                }
            }
        }
    }

    // 计算当前编队的最大纵向偏移（用于确保整队真正处于屏幕外）
    private int getFormationMaxOffsetY() {
        int max = 0;
        for (Point p : formationOffsets) {
            if (p != null && p.y > max) max = p.y;
        }
        return max;
    }

    // 外部可以设置玩家位置，影响俯冲目标
    public void setPlayerPosition(int x, int y) {
        this.playerX = x;
        this.playerY = y;
    }

    // 小队整体运动（核心：按专属轨迹移动，保持编队）
    public void moveSquad() {
        if (!isSpawned || isAllDead) return;

        updateBasePosition();

        for (int slotIndex = 0; slotIndex < enemies.size(); slotIndex++) {
            EnemyAircraft enemy = enemies.get(slotIndex);
            if (!enemy.isAlive()) {
                continue;
            }

            Point baseOffset = slotIndex < formationOffsets.size() ? formationOffsets.get(slotIndex) : new Point(0, 0);
            int formationX = baseX + baseOffset.x;
            int formationY = baseY + baseOffset.y;

            if (enemy instanceof JellyfishAircraft) {
                JellyfishAircraft jelly = (JellyfishAircraft) enemy;
                jelly.setFormationTarget(formationX, formationY);
                jelly.move();
                continue;
            }

            // =====================
            // 保留：俯冲 / 正常编队
            // =====================
            double offsetX = baseOffset.x;
            double offsetY = baseOffset.y;

            DiveInfo d = diveMap.get(enemy);
            if (d != null && d.isDiving) {
                d.progress += d.progressStep;
                if (d.progress > 1.0) d.progress = 1.0;

                if (!d.returning) {
                    double t = d.progress;
                    int diveTargetX = d.diveTargetX;
                    int diveTargetY = d.diveTargetY;
                    offsetX = baseOffset.x * (1 - t) + (diveTargetX - baseXf) * t;
                    offsetY = baseOffset.y * (1 - t) + (diveTargetY - baseYf) * t;
                    if (d.progress >= 1.0) {
                        d.returning = true;
                        d.progress = 0;
                    }
                } else {
                    double t = d.progress;
                    int diveTargetX = d.diveTargetX;
                    int diveTargetY = d.diveTargetY;
                    offsetX = (diveTargetX - baseXf) * (1 - t) + baseOffset.x * t;
                    offsetY = (diveTargetY - baseYf) * (1 - t) + baseOffset.y * t;
                    if (d.progress >= 1.0) {
                        d.reset();
                    }
                }
            }

            enemy.setX((int) (baseXf + offsetX));
            enemy.setY((int) (baseYf + offsetY));

            if (enemy.getY() > panelHeight + 50 && enemy.isAlive()) {
                try {
                    enemy.hit(9999);
                } catch (Throwable ignore) {
                }
            }

            enemy.move();
        }

        checkAllDead();
    }

    // 更新小队敌机子弹
    public void updateSquadBullets() {
        if (!isSpawned) return;

        // 注意：即便敌机已经死亡/小队全灭，也要继续更新其残留子弹，直到子弹自己飞出边界。
        for (EnemyAircraft enemy : enemies) {
            enemy.updateBullets();
        }
    }

    // =====================
    // 小队基准位更新（无脱队版）
    // 目标：敌机从屏幕外进入 -> 上半区巡航 -> 偶尔俯冲/推进 -> 撤退
    // =====================
    private void updateBasePosition() {
        long now = System.currentTimeMillis();
        long elapsed = now - phaseStartTime;

        switch (phase) {
            case ENTER: {
                enterProgress += enterProgressStep;
                if (enterProgress > 1.0) enterProgress = 1.0;

                double t = enterProgress;
                double smoothT = t * t * (3 - 2 * t); // smoothstep

                double curve = Math.sin(Math.PI * smoothT) * ENTER_CURVE_AMPLITUDE;

                // 使用锁定的目标点，避免最后一帧“目标变了”导致瞬移
                double targetX = enterTargetXf;
                double targetY = enterTargetYf;

                baseXf = enterStartXf + (targetX - enterStartXf) * smoothT;
                baseYf = enterStartYf + (targetY - enterStartYf) * smoothT + curve;

                // 不允许低于目标（避免穿过后再回拉造成抽搐）
                if (baseYf > targetY) baseYf = targetY;

                // 入场最后一帧强制精确落点（防止取整/曲线造成的 “差 1px 后突然跳”）
                if (enterProgress >= 1.0) {
                    baseXf = targetX;
                    baseYf = targetY;
                }

                baseX = (int) Math.round(baseXf);
                baseY = (int) Math.round(baseYf);

                if (elapsed >= ENTER_MIN_TIME_MS && baseYf >= 0 && enterProgress >= 1.0) {
                    // 切到巡航前，保证基准点已稳定在目标点
                    baseXf = targetX;
                    baseYf = targetY;
                    baseX = (int) Math.round(baseXf);
                    baseY = (int) Math.round(baseYf);

                    phase = Phase.PATROL;
                    phaseStartTime = now;

                    // 入场后短暂停顿，避免 PATROL 第一帧立即下移
                    postEnterHoldUntil = now + POST_ENTER_HOLD_MS;

                    // 初始化速度为 0
                    velX = 0.0;
                    velY = 0.0;
                    targetVelX = 0.0;
                    targetVelY = 0.0;
                }
                return;
            }
            case PATROL: {
                // 入场后短暂停顿期：保持在位
                if (now < postEnterHoldUntil) {
                    baseX = (int) Math.round(baseXf);
                    baseY = (int) Math.round(baseYf);
                    return;
                }

                // 上半区轻微漂移
                targetVelY = 0.15;
                if (elapsed > 2000 + random.nextInt(2000)) {
                    beginAttackWave();
                    phase = Phase.ATTACK;
                    phaseStartTime = now;
                }
                break;
            }
            case ATTACK: {
                // 缓慢向下推进（让编队有压迫感）
                targetVelY = Math.max(0.9, moveSpeed * 0.40);
                boolean anyDiving = diveMap.values().stream().anyMatch(di -> di.isDiving);
                if (elapsed > 3500 || !anyDiving) {
                    phase = Phase.REGROUP;
                    phaseStartTime = now;
                    // 让未回收的俯冲进入回收阶段
                    diveMap.values().forEach(di -> {
                        if (di.isDiving && !di.returning) {
                            di.returning = true;
                            di.progress = 0;
                        }
                    });
                }
                break;
            }
            case REGROUP: {
                // 回到巡航高度区间
                targetVelY = 0.10;
                boolean anyActive = diveMap.values().stream().anyMatch(di -> di.isDiving);
                if (!anyActive || elapsed > 1800) {
                    phase = random.nextDouble() < 0.2 ? Phase.EXIT : Phase.PATROL;
                    phaseStartTime = now;
                }
                break;
            }
            case EXIT: {
                // 撤退：向下离场
                targetVelY = Math.max(2.2, moveSpeed * 0.65);
                targetVelX *= 0.90;
                break;
            }
        }

        // moveType 决定横向移动趋势（无脱队版，保持克制）
        // 入场时不应用复杂横向趋势，避免一进场就“抖”
        if (phase != Phase.ENTER) {
            switch (moveType) {
                case LEFT_RIGHT: {
                    double sp = Math.max(1.2, moveSpeed * 0.55);
                    if (Math.abs(targetVelX) < 0.1) targetVelX = (random.nextBoolean() ? 1 : -1) * sp;
                    targetVelX = Math.copySign(sp, targetVelX);
                    break;
                }
                case DIAGONAL_BACK_FORTH: {
                    double sp = Math.max(1.05, moveSpeed * 0.42);
                    if (Math.abs(targetVelX) < 0.1) targetVelX = (random.nextBoolean() ? 1 : -1) * sp;
                    targetVelX = Math.copySign(sp, targetVelX);
                    targetVelY += 0.08;
                    break;
                }
                case ZIGZAG: {
                    double sp = Math.max(1.25, moveSpeed * 0.58);
                    targetVelX = Math.sin((now / 260.0) + phaseOffset) * sp;
                    break;
                }
                case SWAY_FORWARD: {
                    targetVelX = Math.sin((now / 520.0) + phaseOffset) * (moveSpeed * 0.55);
                    targetVelY += moveSpeed / 5.0;
                    break;
                }
                case HOVER:
                default:
                    targetVelX *= 0.90;
                    break;
            }
        }

        // 平滑速度
        velX = velX * (1.0 - VEL_SMOOTH) + targetVelX * VEL_SMOOTH;
        velY = velY * (1.0 - VEL_SMOOTH) + targetVelY * VEL_SMOOTH;

        // 轻微扰动：入场阶段禁用扰动，避免敌机在屏幕外被“抖”到 y>=0
        double wobbleX = 0.0;
        double wobbleY = 0.0;
        if (phase != Phase.EXIT && phase != Phase.ENTER) {
            wobbleX = Math.sin((now / 520.0) + phaseOffset) * 0.9;
            wobbleY = Math.cos((now / 900.0) + phaseOffset) * 0.35;
        }

        baseXf += velX + wobbleX;
        baseYf += velY + wobbleY;

        // 边界反弹：入场阶段不做 Y 方向反弹/夹取到 roamMinY，允许其从很负的 y 平滑推进
        if (now - lastBounceTime > BOUNCE_COOLDOWN_MS) {
            boolean bounced = false;
            if (baseXf < boundary) {
                baseXf = boundary + 0.8;
                velX = Math.abs(velX);
                targetVelX = Math.abs(targetVelX);
                bounced = true;
            } else if (baseXf > panelWidth - boundary) {
                baseXf = panelWidth - boundary - 0.8;
                velX = -Math.abs(velX);
                targetVelX = -Math.abs(targetVelX);
                bounced = true;
            }

            if (phase != Phase.EXIT && phase != Phase.ENTER) {
                double minY = Math.max(-200, roamMinY);
                double maxY = Math.min(COMBAT_MAX_Y, roamMaxY);
                if (baseYf < minY) {
                    baseYf = minY + 0.8;
                    velY = Math.abs(velY);
                    targetVelY = Math.abs(targetVelY);
                    bounced = true;
                } else if (baseYf > maxY) {
                    baseYf = maxY - 0.8;
                    velY = -Math.abs(velY) * 0.75;
                    targetVelY = -Math.abs(targetVelY) * 0.55;
                    bounced = true;
                }
            }

            if (bounced) lastBounceTime = now;
        }

        // 兜底 clamp
        baseXf = Math.max(boundary, Math.min(panelWidth - boundary, baseXf));

        // 入场阶段允许更高（更负）的 y；不要 clamp 到 -200
        if (phase == Phase.ENTER) {
            baseYf = Math.min(panelHeight + 300, baseYf);
        } else {
            baseYf = Math.max(-200, Math.min(panelHeight + 300, baseYf));
        }

        if (phase != Phase.EXIT) baseYf = Math.min(baseYf, COMBAT_MAX_Y);

        baseX = (int) baseXf;
        baseY = (int) baseYf;

        // EXIT 阶段越界则直接判定离场
        if (phase == Phase.EXIT && baseYf > panelHeight + 70) {
            for (EnemyAircraft e : enemies) {
                if (e != null && e.isAlive()) {
                    try { e.hit(9999); } catch (Throwable ignore) {}
                }
            }
        }
    }

    // 检查小队是否全灭
    private void checkAllDead() {
        isAllDead = enemies.stream().noneMatch(EnemyAircraft::isAlive);
    }

    // 发起一次攻击浪：随机挑选若干架敌机进行俯冲（无脱队版）
    private void beginAttackWave() {
        // 选择1到min(3,alive)架敌机进行俯冲（不包含 Jellyfish）
        List<EnemyAircraft> alive = new ArrayList<>();
        for (EnemyAircraft e : enemies) {
            if (e == null || !e.isAlive()) continue;
            if (e instanceof JellyfishAircraft) continue;
            alive.add(e);
        }
        if (alive.isEmpty()) return;

        int maxDivers = Math.min(3, alive.size());
        int divers = 1 + random.nextInt(maxDivers);

        for (int i = 0; i < divers; i++) {
            EnemyAircraft e = alive.get(random.nextInt(alive.size()));
            DiveInfo di = diveMap.get(e);
            if (di == null) {
                di = new DiveInfo();
                diveMap.put(e, di);
            }
            if (di.isDiving) continue;

            di.isDiving = true;
            di.returning = false;
            di.progress = 0.0;

            // 更平滑的俯冲：步进更小一点，整体时间更长，视觉更像“曲线俯冲”
            di.progressStep = 0.010 + random.nextDouble() * 0.006;

            // 按你的要求：去掉“同步玩家位置”。
            // 俯冲目标改为：基于当前编队位置向下推进 + 水平侧移（更像雷霆战机的压迫俯冲）
            int baseCenterX = (int) Math.round(baseXf);
            int baseCenterY = (int) Math.round(baseYf);

            int side = random.nextBoolean() ? 1 : -1;
            int lateral = (70 + random.nextInt(120)) * side; // 70~189 像素侧移
            di.diveTargetX = Math.max(boundary, Math.min(panelWidth - boundary, baseCenterX + lateral));

            int depth = 260 + random.nextInt(200); // 向下推进深度
            int targetY = baseCenterY + depth;
            di.diveTargetY = Math.min(targetY, DIVE_MAX_Y);
        }
    }

    // Getter & Setter
    public List<EnemyAircraft> getEnemies() {
        // 未 spawn/未创建时不返回任何敌机，避免渲染/碰撞等逻辑误用
        if (!isSpawned || !enemiesCreated) return java.util.Collections.emptyList();
        return enemies;
    }
    public boolean isSpawned() { return isSpawned; }
    public boolean isAllDead() { return isAllDead; }
    public int getSquadId() { return squadId; }
    public EnemyMoveType getMoveType() { return moveType; }
    public Formation getFormation() { return formation; }

    // 内部：表示单架敌机的俯冲状态
    private static class DiveInfo {
        boolean isDiving = false;
        boolean returning = false;
        double progress = 0.0;
        double progressStep = 0.06;
        int diveTargetX = 0;
        int diveTargetY = 0;

        void reset() {
            isDiving = false;
            returning = false;
            progress = 0.0;
            progressStep = 0.06;
            diveTargetX = 0;
            diveTargetY = 0;
        }
    }

    // 入场结束到巡航开始之间的短暂停顿（防止切到 PATROL 后第一帧立刻下移造成“到位瞬移”）
    private static final long POST_ENTER_HOLD_MS = 180;
    private long postEnterHoldUntil = 0L;
}
