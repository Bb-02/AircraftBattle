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
    private int baseX, baseY;           // 小队基准位置（编队中心）
    private int targetBaseY;            // 进入阶段目标Y
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
    private int panelHeight = 600;
    private int boundary = 50;

    // 玩家位置（由外部设置，用于俯冲目标）
    private volatile int playerX = panelWidth / 2;
    private volatile int playerY = panelHeight - 100;

    // 记录编队初始偏移，便于重组
    private List<Point> formationOffsets = new ArrayList<>();

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
        this.moveSpeed = 2 + waveNumber;
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

        this.enemies = new ArrayList<>();
        generateSquadEnemies();

        this.phase = Phase.ENTER;
        this.phaseStartTime = System.currentTimeMillis();
    }

    // 按编队形状生成小队敌机（同时记录初始偏移）
    private void generateSquadEnemies() {
        int spacing = 40; // 编队间距（敌机之间的距离）

        formationOffsets.clear();

        switch (formation) {
            case HORIZONTAL:
                for (int i = 0; i < enemyCount; i++) {
                    int offsetX = - (enemyCount - 1) * spacing / 2 + i * spacing;
                    int offsetY = 0;
                    formationOffsets.add(new Point(offsetX, offsetY));
                    addEnemy(baseX + offsetX, baseY + offsetY);
                }
                break;
            case VERTICAL:
                for (int i = 0; i < enemyCount; i++) {
                    int offsetX = 0;
                    int offsetY = - (enemyCount - 1) * spacing / 2 + i * spacing;
                    formationOffsets.add(new Point(offsetX, offsetY));
                    addEnemy(baseX + offsetX, baseY + offsetY);
                }
                break;
            case TRIANGLE:
                // 固定最多5个位置
                Point[] tri = {
                        new Point(0,0),
                        new Point(-spacing, spacing),
                        new Point(spacing, spacing),
                        new Point(0, -spacing),
                        new Point(0, 2*spacing)
                };
                for (int i = 0; i < enemyCount; i++) {
                    formationOffsets.add(new Point(tri[i]));
                    addEnemy(baseX + tri[i].x, baseY + tri[i].y);
                }
                break;
            case DIAMOND:
                Point[] dia = {
                        new Point(0,0),
                        new Point(-spacing,0),
                        new Point(spacing,0),
                        new Point(0,-spacing),
                        new Point(0,spacing)
                };
                for (int i = 0; i < enemyCount; i++) {
                    formationOffsets.add(new Point(dia[i]));
                    addEnemy(baseX + dia[i].x, baseY + dia[i].y);
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
        EnemyAircraft enemy = new EnemyAircraft(
                800, 600, moveType, waveNumber, x, y // 传递初始位置，适配小队编队
        );
        enemies.add(enemy);
    }

    // 检查小队是否到生成时间
    public void checkSpawn(long waveStartTime) {
        if (!isSpawned && System.currentTimeMillis() - waveStartTime >= spawnDelay) {
            isSpawned = true;
            spawnTime = System.currentTimeMillis();
            phase = Phase.ENTER;
            phaseStartTime = System.currentTimeMillis();
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

        int idx = 0;
        for (EnemyAircraft enemy : enemies) {
            if (!enemy.isAlive()) {
                idx++;
                continue;
            }

            Point baseOffset = idx < formationOffsets.size() ? formationOffsets.get(idx) : new Point(0,0);
            int offsetX = baseOffset.x;
            int offsetY = baseOffset.y;

            DiveInfo d = diveMap.get(enemy);
            if (d != null && d.isDiving) {
                d.progress += d.progressStep;
                if (d.progress > 1.0) d.progress = 1.0;

                if (!d.returning) {
                    double t = d.progress;
                    int diveTargetX = d.diveTargetX;
                    int diveTargetY = d.diveTargetY;
                    offsetX = (int) (baseOffset.x * (1 - t) + (diveTargetX - baseX) * t);
                    offsetY = (int) (baseOffset.y * (1 - t) + (diveTargetY - baseY) * t);
                    if (d.progress >= 1.0) {
                        d.returning = true;
                        d.progress = 0;
                    }
                } else {
                    double t = d.progress;
                    int diveTargetX = d.diveTargetX;
                    int diveTargetY = d.diveTargetY;
                    offsetX = (int) ((diveTargetX - baseX) * (1 - t) + baseOffset.x * t);
                    offsetY = (int) ((diveTargetY - baseY) * (1 - t) + baseOffset.y * t);
                    if (d.progress >= 1.0) {
                        d.reset();
                    }
                }
            }

            enemy.setX(baseX + offsetX + random.nextInt(3) - 1);
            enemy.setY(baseY + offsetY + random.nextInt(3) - 1);

            // 如果敌机已经飞出屏幕底部，直接标记为死亡，避免波次卡住
            if (enemy.getY() > panelHeight + 50 && enemy.isAlive()) {
                try {
                    enemy.hit(9999); // 使用大伤害保证死亡（兼容没有 setAlive 接口的实现）
                } catch (Throwable ignore) {
                    // 若 hit 不存在或抛异常则忽略（项目中一般存在 hit(int)）
                }
            }

            enemy.move();
            idx++;
        }

        checkAllDead();
    }

    // 更新小队基准位置（按运动类型和阶段）
    private void updateBasePosition() {
        long now = System.currentTimeMillis();
        long phaseElapsed = now - phaseStartTime;

        // 阶段机：ENTER -> PATROL -> ATTACK 循环 -> EXIT
        switch (phase) {
            case ENTER:
                // 平滑进入目标Y，同时做左右小摆动
                int enterSpeed = Math.max(1, moveSpeed / 2);
                if (baseY < targetBaseY) baseY += enterSpeed;
                baseX += (int) (Math.sin(now / 600.0) * 2);
                if (baseY >= targetBaseY) {
                    phase = Phase.PATROL;
                    phaseStartTime = now;
                }
                break;
            case PATROL:
                // 在上半区巡航，左右摆动，停留一段时间后触发攻击
                baseX += (int) (Math.sin(now / 500.0) * moveSpeed * 1.5);
                baseY += (int) (Math.cos(now / 1200.0) * 0.2);
                if (phaseElapsed > 2000 + random.nextInt(2000)) { // 巡航2-4秒后开始攻击
                    beginAttackWave();
                    phase = Phase.ATTACK;
                    phaseStartTime = now;
                }
                break;
            case ATTACK:
                // 攻击时基准点缓慢向下并左右摆动幅度增大
                baseY += Math.max(1, moveSpeed / 3);
                baseX += (int) (Math.sin(now / 300.0) * moveSpeed * 2);
                // 若攻击持续过长或多数敌机正在返回则进入REGROUP
                boolean anyDiving = diveMap.values().stream().anyMatch(di -> di.isDiving);
                if (phaseElapsed > 3000 + random.nextInt(2000) || !anyDiving) {
                    phase = Phase.REGROUP;
                    phaseStartTime = now;
                    // 强制所有未返回的俯冲开始返回
                    diveMap.values().forEach(di -> {
                        if (di.isDiving && !di.returning) {
                            di.returning = true;
                            di.progress = 0;
                        }
                    });
                }
                break;
            case REGROUP:
                // 稳定位置等待所有回归到编队
                baseX += (int) (Math.sin(now / 800.0) * moveSpeed);
                // 检查是否全部回归（所有 DiveInfo 都 inactive）
                boolean anyActive = diveMap.values().stream().anyMatch(di -> di.isDiving);
                if (!anyActive || phaseElapsed > 2000) {
                    // 小概率直接退出或回到巡航
                    if (random.nextDouble() < 0.2) {
                        phase = Phase.EXIT;
                    } else {
                        phase = Phase.PATROL;
                    }
                    phaseStartTime = now;
                }
                break;
            case EXIT:
                // 缓慢向下撤退离开屏幕
                baseY += Math.max(2, moveSpeed / 2);
                baseX += (int) (Math.sin(now / 700.0) * moveSpeed);
                if (baseY > panelHeight + 100) {
                    // 让所有敌机离场并标记为死亡，避免无法被玩家击中却仍被判活着
                    enemies.forEach(e -> {
                        e.setY(baseY + 200);
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
                baseX += random.nextInt(3) - 1;
                baseY += random.nextInt(2) - 1;
                break;
            case LEFT_RIGHT:
                baseX += moveDirection * moveSpeed;
                if (baseX <= boundary || baseX >= panelWidth - boundary) moveDirection *= -1;
                break;
            case DIAGONAL_BACK_FORTH:
                baseX += moveDirection * moveSpeed;
                baseY += moveDirection * moveSpeed;
                if (baseX <= boundary || baseX >= panelWidth - boundary ||
                        baseY <= boundary || baseY >= 300) moveDirection *= -1;
                break;
            case SPIRAL:
                spiralAngle += 0.1 * moveSpeed;
                baseX += (int) (Math.cos(spiralAngle) * Math.max(1, moveSpeed / 2));
                baseY += (int) (Math.sin(spiralAngle) * Math.max(1, moveSpeed / 2)) + 1;
                if (baseX <= boundary || baseX >= panelWidth - boundary) spiralAngle += Math.PI;
                if (baseY >= 350) baseY = 350;
                break;
            case SWAY_FORWARD:
                baseX += (int) (Math.sin(System.currentTimeMillis() / 500.0) * moveSpeed * 2);
                baseY += moveSpeed / 4;
                baseX = Math.max(boundary, Math.min(panelWidth - boundary, baseX));
                break;
            case ZIGZAG:
                zigzagStep++;
                if (zigzagStep % 30 == 0) zigzagDir *= -1;
                baseX += zigzagDir * moveSpeed;
                baseY += moveSpeed / 2;
                if (baseX <= boundary || baseX >= panelWidth - boundary) zigzagDir *= -1;
                if (baseY >= 350) baseY = 350;
                break;
        }

        // 兜底：确保基准位置在屏幕内合理范围
        baseX = Math.max(boundary, Math.min(panelWidth - boundary, baseX));
        baseY = Math.max(-200, Math.min(panelHeight + 300, baseY)); // 允许进入和退出的Y范围
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
                di.progressStep = 0.03 + random.nextDouble() * 0.01; // 控制速度
                // 目标在玩家附近，带一点随机偏差与前导
                di.diveTargetX = playerX + random.nextInt(120) - 60;
                di.diveTargetY = Math.min(playerY + 20 + random.nextInt(80), panelHeight - 50);
            }
        }
    }

    // 检查小队是否全灭
    private void checkAllDead() {
        isAllDead = enemies.stream().noneMatch(EnemyAircraft::isAlive);
    }

    // 更新小队敌机子弹
    public void updateSquadBullets() {
        if (!isSpawned || isAllDead) return;
        for (EnemyAircraft enemy : enemies) {
            if (enemy.isAlive()) {
                enemy.updateBullets();
            }
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