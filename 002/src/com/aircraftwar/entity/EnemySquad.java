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
    private long spawnDelay;            // 生成延迟（波内小队的生成间隔）
    private long spawnTime;             // 小队生成时间
    private int baseX, baseY;           // 小队基准位置（编队中心，用于渲染/外部读取）
    private double baseXf, baseYf;      // 新增：小队基准位置（double 缓冲，消除抖动）
    private int targetBaseY;            // 进入阶段目标Y
    private double targetBaseYf;        // 新增：进入阶段目标Y（double 缓冲）
    private int moveSpeed;              // 小队移动速度（随波次递增）
    private boolean isSpawned;          // 是否已生成
    private boolean isAllDead;          // 小队是否全灭

    // 运动参数（适配不同轨迹）
    private int moveDirection = 1;      // 移动方向（1=右/下，-1=左/上）
    private double spiralAngle = 0;     // 螺旋角度
    private int zigzagStep = 0;         // Z型折线步数
    private int zigzagDir = 1;          // Z型折线方向

    private static final Random random = new Random();

    // 新增：分阶段状态与每架敌机俯冲状态
    private Phase phase = Phase.ENTER;
    private long phaseStartTime;
    private Map<EnemyAircraft, DiveInfo> diveMap = new HashMap<>();
    private int panelWidth = 800;
    private int panelHeight = 850;
    private int boundary = 50;

    // 玩家位置（由外部设置，用于俯冲目标）
    private volatile int playerX = panelWidth / 2;
    private volatile int playerY = panelHeight - 100;

    // 记录编队初始偏移，便于重组
    private List<Point> formationOffsets = new ArrayList<>();

    // 新增：每个小队一个相位偏移，避免所有队形同步导致视觉僵硬
    private final double phaseOffset = random.nextDouble() * Math.PI * 2;

    // 新增：普通阶段敌机活动的最低高度（y 越大越靠近底部）；避免玩家打不到
    private static final int COMBAT_MAX_Y = 700;

    // 方案B：俯冲允许更低一点，但仍然不贴底，确保玩家基本还能打到
    private static final int DIVE_MAX_Y = 760;

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
            int typeIdx = random.nextInt(3);
            switch (typeIdx) {
                case 0: this.moveType = EnemyMoveType.DIAGONAL_BACK_FORTH; break;
                case 1: this.moveType = EnemyMoveType.SPIRAL; break;
                case 2: this.moveType = EnemyMoveType.ZIGZAG; break;
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
        this.baseY = -40 - random.nextInt(200);  // 从上面外侧进入
        this.targetBaseY = random.nextInt(100) + 80; // 到达进入目标Y（屏幕上半区）

        // 同步 double 缓冲
        this.baseXf = this.baseX;
        this.baseYf = this.baseY;
        this.targetBaseYf = this.targetBaseY;

        this.enemies = new ArrayList<>();
        generateSquadEnemies();

        this.phase = Phase.ENTER;
        this.phaseStartTime = System.currentTimeMillis();
    }

    // 按编队形状生成小队敌机（同时记录初始偏移）
    private void generateSquadEnemies() {
        int spacingX = 46; // X 间距（更自然）
        int spacingY = 36; // Y 间距（更自然）

        formationOffsets.clear();

        // 更自然的“交错队列”：两列错位排列（类似交错编队/蜂群）
        // enemyCount: 1-5
        // slot 0: (0,0)
        // slot 1: (-spacingX/2, spacingY)
        // slot 2: ( spacingX/2, spacingY)
        // slot 3: (-spacingX/2, 2*spacingY)
        // slot 4: ( spacingX/2, 2*spacingY)
        Point[] stagger = {
                new Point(0, 0),
                new Point(-spacingX / 2, spacingY),
                new Point(spacingX / 2, spacingY),
                new Point(-spacingX / 2, 2 * spacingY),
                new Point(spacingX / 2, 2 * spacingY)
        };

        // 仍保留你原来的 formation 选择，但让布局更“自然”；
        // HORIZONTAL/DIAMOND/默认 => 交错队列
        // VERTICAL => 轻微左右交错的竖列
        // TRIANGLE => 三角（轻微收紧）
        switch (formation) {
            case VERTICAL:
                for (int i = 0; i < enemyCount; i++) {
                    int ox = (i % 2 == 0) ? 0 : spacingX / 3; // 竖列左右轻微错位
                    int oy = i * spacingY;
                    formationOffsets.add(new Point(ox, oy));
                    addEnemy(baseX + ox, baseY + oy);
                }
                break;
            case TRIANGLE:
                // 三角：顶点 + 两侧 + 底部（最多 5）
                Point[] tri = {
                        new Point(0, 0),
                        new Point(-spacingX / 2, spacingY),
                        new Point(spacingX / 2, spacingY),
                        new Point(0, 2 * spacingY),
                        new Point(0, 3 * spacingY)
                };
                for (int i = 0; i < enemyCount; i++) {
                    formationOffsets.add(new Point(tri[i]));
                    addEnemy(baseX + tri[i].x, baseY + tri[i].y);
                }
                break;
            case HORIZONTAL:
            case DIAMOND:
            default:
                for (int i = 0; i < enemyCount; i++) {
                    formationOffsets.add(new Point(stagger[i]));
                    addEnemy(baseX + stagger[i].x, baseY + stagger[i].y);
                }
                break;
        }

        // 初始化每架敌机的俯冲状态
        diveMap.clear();
        for (EnemyAircraft e : enemies) {
            diveMap.put(e, new DiveInfo());
        }
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
                    800, 600, moveType, waveNumber, x, y // 传递初始位置，适配小队编队
            );
        }
        enemies.add(enemy);
    }

    // 检查小队是否到生成时间
    public void checkSpawn(long waveStartTime) {
        if (!isSpawned && System.currentTimeMillis() - waveStartTime >= spawnDelay) {
            isSpawned = true;
            spawnTime = System.currentTimeMillis();
            phase = Phase.ENTER;
            phaseStartTime = System.currentTimeMillis();

            // 重新生成/进入时，同步 double 缓冲，避免首帧跳变
            this.baseXf = this.baseX;
            this.baseYf = this.baseY;
            this.targetBaseYf = this.targetBaseY;
        }
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
            int targetX = baseX + baseOffset.x;
            int targetY = baseY + baseOffset.y;

            if (enemy instanceof JellyfishAircraft) {
                JellyfishAircraft jelly = (JellyfishAircraft) enemy;
                jelly.setFormationTarget(targetX, targetY);
                jelly.move();
                continue;
            }

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
                    // 用 double 插值再 round，减少 int 截断造成的“跳动”
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

            enemy.setX((int) Math.round(baseXf + offsetX));
            enemy.setY((int) Math.round(baseYf + offsetY));

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

    // 更新小队基准位置（按运动类型和阶段）
    private void updateBasePosition() {
        long now = System.currentTimeMillis();
        long phaseElapsed = now - phaseStartTime;

        // 阶段机：ENTER -> PATROL -> ATTACK 循环 -> EXIT
        switch (phase) {
            case ENTER: {
                double enterSpeed = Math.max(1.0, moveSpeed / 2.0);
                if (baseYf < targetBaseYf) baseYf += enterSpeed;
                // ENTER 阶段不再叠加额外的 baseXf 大摆动，避免与 moveType 叠加造成跳变
                if (baseYf >= targetBaseYf) {
                    phase = Phase.PATROL;
                    phaseStartTime = now;
                }
                break;
            }
            case PATROL:
                // PATROL 阶段只做很轻微的纵向漂移，让 moveType 来负责“轨迹风格”
                baseYf += Math.cos(now / 1200.0) * 0.20;
                if (phaseElapsed > 2000 + random.nextInt(2000)) {
                    beginAttackWave();
                    phase = Phase.ATTACK;
                    phaseStartTime = now;
                }
                break;
            case ATTACK:
                // ATTACK 阶段主要向下推进，横向由 moveType 决定
                baseYf += Math.max(1.0, moveSpeed / 3.0);
                boolean anyDiving = diveMap.values().stream().anyMatch(di -> di.isDiving);
                if (phaseElapsed > 3000 + random.nextInt(2000) || !anyDiving) {
                    phase = Phase.REGROUP;
                    phaseStartTime = now;
                    diveMap.values().forEach(di -> {
                        if (di.isDiving && !di.returning) {
                            di.returning = true;
                            di.progress = 0;
                        }
                    });
                }
                break;
            case REGROUP:
                // REGROUP 阶段保持纵向基本稳定
                boolean anyActive = diveMap.values().stream().anyMatch(di -> di.isDiving);
                if (!anyActive || phaseElapsed > 2000) {
                    phase = random.nextDouble() < 0.2 ? Phase.EXIT : Phase.PATROL;
                    phaseStartTime = now;
                }
                break;
            case EXIT:
                baseYf += Math.max(2.0, moveSpeed / 2.0);
                if (baseYf > panelHeight + 50) {
                    enemies.forEach(e -> {
                        e.setY((int) Math.round(baseYf + 200));
                        if (e.isAlive()) {
                            try {
                                e.hit(9999);
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                }
                break;
        }

        // 根据 moveType 增强基准位移动（保留原有风格）
        switch (moveType) {
            case HOVER:
                baseXf += Math.sin((now / 700.0) + phaseOffset) * 1.2;
                baseYf += Math.cos((now / 900.0) + phaseOffset) * 0.6;
                break;
            case LEFT_RIGHT:
                baseXf += moveDirection * (double) moveSpeed;
                if (baseXf <= boundary || baseXf >= panelWidth - boundary) moveDirection *= -1;
                break;
            case DIAGONAL_BACK_FORTH: {
                // 让斜向移动更“聪明”：速度稍慢 + 轻微摆动扰动
                double sp = Math.max(1.4, moveSpeed * 0.75);
                baseXf += moveDirection * sp;
                baseYf += moveDirection * (sp * 0.70);
                // 小扰动，避免呆板
                baseXf += Math.sin((now / 280.0) + phaseOffset) * 0.35;
                baseYf += Math.cos((now / 360.0) + phaseOffset) * 0.18;

                if (baseXf <= boundary || baseXf >= panelWidth - boundary ||
                        baseYf <= boundary || baseYf >= 300) moveDirection *= -1;
                break;
            }
            case SPIRAL:
                spiralAngle += 0.07 * moveSpeed;
                baseXf += Math.cos(spiralAngle) * Math.max(0.8, moveSpeed / 2.2);
                baseYf += Math.sin(spiralAngle) * Math.max(0.8, moveSpeed / 2.2) + 0.9;
                if (baseXf <= boundary || baseXf >= panelWidth - boundary) spiralAngle += Math.PI;
                if (baseYf >= 350) baseYf = 350;
                break;
            case SWAY_FORWARD:
                baseXf += Math.sin((now / 520.0) + phaseOffset) * moveSpeed * 1.7;
                baseYf += moveSpeed / 4.5;
                baseXf = Math.max(boundary, Math.min(panelWidth - boundary, baseXf));
                break;
            case ZIGZAG:
                // 原实现是每30帧硬切换方向，会产生明显“顿挫/抽搐”
                // 改成时间驱动的平滑折线摆动（视觉更智能也更顺滑）
                baseXf += Math.sin((now / 260.0) + phaseOffset) * moveSpeed * 2.0;
                baseYf += moveSpeed / 2.6;
                if (baseYf >= 350) baseYf = 350;
                break;
        }

        // clamp（double）
        baseXf = Math.max(boundary, Math.min(panelWidth - boundary, baseXf));
        baseYf = Math.max(-200, Math.min(panelHeight + 300, baseYf));

        // 关键：普通阶段不允许走到屏幕底部不可攻击区（除非 EXIT 撤退/俯冲目标）
        if (phase != Phase.EXIT) {
            baseYf = Math.min(baseYf, COMBAT_MAX_Y);
        }

        // 同步回 int（唯一一次 round）
        baseX = (int) Math.round(baseXf);
        baseY = (int) Math.round(baseYf);
    }

    // 发起一次攻击浪：随机挑选若干架敌机进行俯冲
    private void beginAttackWave() {
        // 选择1到min(3,alive)架敌机进行俯冲
        List<EnemyAircraft> alive = new ArrayList<>();
        for (EnemyAircraft e : enemies) if (e.isAlive()) alive.add(e);
        if (alive.isEmpty()) return;

        int maxDivers = Math.min(3, alive.size());
        int divers = 1 + random.nextInt(maxDivers);
        for (int i = 0; i < divers; i++) {
            EnemyAircraft e = alive.get(random.nextInt(alive.size()));
            DiveInfo di = diveMap.get(e);
            if (di != null && !di.isDiving) {
                di.isDiving = true;
                di.returning = false;
                di.progress = 0;
                di.progressStep = 0.012 + random.nextDouble() * 0.008;
                di.diveTargetX = playerX + random.nextInt(120) - 60;

                // 俯冲目标Y：限制到 DIVE_MAX_Y，避免贴底导致玩家打不到
                int targetY = playerY + 20 + random.nextInt(80);
                di.diveTargetY = Math.min(targetY, DIVE_MAX_Y);
            }
        }
    }

    // 检查小队是否全灭
    private void checkAllDead() {
        isAllDead = enemies.stream().noneMatch(EnemyAircraft::isAlive);
    }

    // 更新小队敌机子弹
    public void updateSquadBullets() {
        if (!isSpawned) return;

        // 注意：即便敌机已经死亡/小队全灭，也要继续更新其残留子弹，直到子弹自己飞出边界。
        for (EnemyAircraft enemy : enemies) {
            enemy.updateBullets();
        }
    }

    // Getter & Setter
    public List<EnemyAircraft> getEnemies() { return enemies; }
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
}
