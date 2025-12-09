package com.aircraftwar.entity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 敌机小队类（参考雷霆战机编队逻辑）
 */
public class EnemySquad {
    // 编队形状枚举
    public enum Formation {
        HORIZONTAL, // 横排（左右排列）
        VERTICAL,   // 竖排（上下排列）
        TRIANGLE,   // 三角编队
        DIAMOND     // 菱形编队
    }

    private int squadId;                // 小队编号
    private Formation formation;        // 编队形状
    private EnemyMoveType moveType;     // 小队运动类型
    private int enemyCount;             // 小队敌机数量
    private int waveNumber;             // 所属波次
    private List<EnemyAircraft> enemies;// 小队敌机列表
    private long spawnDelay;            // 生成延迟（波内小队的生成间隔）
    private long spawnTime;             // 小队生成时间
    private int baseX, baseY;           // 小队基准位置（编队中心）
    private int moveSpeed;              // 小队移动速度（随波次递增）
    private boolean isSpawned;          // 是否已生成
    private boolean isAllDead;          // 小队是否全灭

    // 运动参数（适配不同轨迹）
    private int moveDirection = 1;      // 移动方向（1=右/下，-1=左/上）
    private double spiralAngle = 0;     // 螺旋角度
    private int zigzagStep = 0;         // Z型折线步数
    private int zigzagDir = 1;          // Z型折线方向

    private static final Random random = new Random();

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
        double complexProb = Math.min(0.1 * waveNumber, 0.9); // 波次越高，复杂运动概率越高（最高90%）
        if (random.nextDouble() < complexProb) {
            // 复杂运动类型（雷霆战机核心轨迹）
            int typeIdx = random.nextInt(3);
            switch (typeIdx) {
                case 0: this.moveType = EnemyMoveType.DIAGONAL_BACK_FORTH; break;
                case 1: this.moveType = EnemyMoveType.SPIRAL; break;
                case 2: this.moveType = EnemyMoveType.ZIGZAG; break;
                default: this.moveType = EnemyMoveType.SWAY_FORWARD; break;
            }
        } else {
            // 基础运动类型
            this.moveType = random.nextBoolean() ? EnemyMoveType.LEFT_RIGHT : EnemyMoveType.HOVER;
        }

        // 随机选择编队形状
        int formationIdx = random.nextInt(4);
        switch (formationIdx) {
            case 0: this.formation = Formation.HORIZONTAL; break;
            case 1: this.formation = Formation.VERTICAL; break;
            case 2: this.formation = Formation.TRIANGLE; break;
            default: this.formation = Formation.DIAMOND; break;
        }

        // 初始化小队基准位置（屏幕上半部分，避免初始越界）
        this.baseX = random.nextInt(600) + 100; // X: 100-700
        this.baseY = random.nextInt(100) + 50;  // Y: 50-150（雷霆战机经典上半区）

        // 初始化敌机列表（按编队形状生成）
        this.enemies = new ArrayList<>();
        generateSquadEnemies();
    }

    // 按编队形状生成小队敌机
    private void generateSquadEnemies() {
        int spacing = 40; // 编队间距（敌机之间的距离）
        int enemyWidth = 30;
        int enemyHeight = 40;

        switch (formation) {
            case HORIZONTAL: // 横排（左右排列）
                for (int i = 0; i < enemyCount; i++) {
                    int x = baseX - (enemyCount - 1) * spacing / 2 + i * spacing;
                    int y = baseY;
                    addEnemy(x, y);
                }
                break;
            case VERTICAL: // 竖排（上下排列）
                for (int i = 0; i < enemyCount; i++) {
                    int x = baseX;
                    int y = baseY - (enemyCount - 1) * spacing / 2 + i * spacing;
                    addEnemy(x, y);
                }
                break;
            case TRIANGLE: // 三角编队（中心+上下左右）
                addEnemy(baseX, baseY); // 中心
                if (enemyCount >= 2) addEnemy(baseX - spacing, baseY + spacing); // 左下
                if (enemyCount >= 3) addEnemy(baseX + spacing, baseY + spacing); // 右下
                if (enemyCount >= 4) addEnemy(baseX, baseY - spacing); // 上
                if (enemyCount >= 5) addEnemy(baseX, baseY + 2*spacing); // 下
                break;
            case DIAMOND: // 菱形编队
                addEnemy(baseX, baseY); // 中心
                if (enemyCount >= 2) addEnemy(baseX - spacing, baseY); // 左
                if (enemyCount >= 3) addEnemy(baseX + spacing, baseY); // 右
                if (enemyCount >= 4) addEnemy(baseX, baseY - spacing); // 上
                if (enemyCount >= 5) addEnemy(baseX, baseY + spacing); // 下
                break;
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
        }
    }

    // 小队整体运动（核心：按专属轨迹移动，保持编队）
    public void moveSquad() {
        if (!isSpawned || isAllDead) return;

        // 更新小队基准位置（按运动类型）
        updateBasePosition();

        // 同步小队内所有敌机的位置（保持编队相对位置）
        int spacing = 40;
        int idx = 0;
        for (EnemyAircraft enemy : enemies) {
            if (!enemy.isAlive()) continue;

            // 基于基准位置的编队偏移
            int offsetX = 0, offsetY = 0;
            switch (formation) {
                case HORIZONTAL:
                    offsetX = - (enemyCount - 1) * spacing / 2 + idx * spacing;
                    break;
                case VERTICAL:
                    offsetY = - (enemyCount - 1) * spacing / 2 + idx * spacing;
                    break;
                case TRIANGLE:
                    if (idx == 0) { offsetX=0; offsetY=0; }
                    else if (idx == 1) { offsetX=-spacing; offsetY=spacing; }
                    else if (idx == 2) { offsetX=spacing; offsetY=spacing; }
                    else if (idx == 3) { offsetX=0; offsetY=-spacing; }
                    else if (idx == 4) { offsetX=0; offsetY=2*spacing; }
                    break;
                case DIAMOND:
                    if (idx == 0) { offsetX=0; offsetY=0; }
                    else if (idx == 1) { offsetX=-spacing; offsetY=0; }
                    else if (idx == 2) { offsetX=spacing; offsetY=0; }
                    else if (idx == 3) { offsetX=0; offsetY=-spacing; }
                    else if (idx == 4) { offsetX=0; offsetY=spacing; }
                    break;
            }

            // 更新敌机位置（基准位置+编队偏移+微小随机晃动）
            enemy.setX(baseX + offsetX + random.nextInt(3) - 1);
            enemy.setY(baseY + offsetY + random.nextInt(3) - 1);

            // 敌机自身移动（射击+微小调整）
            enemy.move();
            idx++;
        }

        // 检查小队是否全灭
        checkAllDead();
    }

    // 更新小队基准位置（按运动类型）
    private void updateBasePosition() {
        int panelWidth = 800;
        int panelHeight = 600;
        int boundary = 50; // 边界留白

        switch (moveType) {
            case HOVER:
                // 定点巡航：微小晃动
                baseX += random.nextInt(3) - 1;
                baseY += random.nextInt(3) - 1;
                break;
            case LEFT_RIGHT:
                // 左右来回
                baseX += moveDirection * moveSpeed;
                if (baseX <= boundary || baseX >= panelWidth - boundary) {
                    moveDirection *= -1; // 反向
                }
                break;
            case DIAGONAL_BACK_FORTH:
                // 斜向来回（45度）
                baseX += moveDirection * moveSpeed;
                baseY += moveDirection * moveSpeed;
                if (baseX <= boundary || baseX >= panelWidth - boundary ||
                        baseY <= boundary || baseY >= 300) { // 限制在屏幕上半区
                    moveDirection *= -1;
                }
                break;
            case SPIRAL:
                // 螺旋移动（绕基准点旋转+缓慢前进）
                spiralAngle += 0.1 * moveSpeed;
                baseX += (int) (Math.cos(spiralAngle) * moveSpeed);
                baseY += (int) (Math.sin(spiralAngle) * moveSpeed) + 1; // 缓慢向下
                // 边界回弹
                if (baseX <= boundary || baseX >= panelWidth - boundary) {
                    spiralAngle += Math.PI; // 反向旋转
                }
                if (baseY >= 350) {
                    baseY = 350; // 限制下边界
                }
                break;
            case SWAY_FORWARD:
                // 左右摇摆+缓慢前进（向下）
                baseX += (int) (Math.sin(System.currentTimeMillis() / 500.0) * moveSpeed * 2);
                baseY += moveSpeed / 2;
                // 限制X边界
                baseX = Math.max(boundary, Math.min(panelWidth - boundary, baseX));
                break;
            case ZIGZAG:
                // Z型折线移动
                zigzagStep++;
                if (zigzagStep % 30 == 0) { // 每30帧变向
                    zigzagDir *= -1;
                }
                baseX += zigzagDir * moveSpeed;
                baseY += moveSpeed;
                // 边界回弹
                if (baseX <= boundary || baseX >= panelWidth - boundary) {
                    zigzagDir *= -1;
                }
                if (baseY >= 350) {
                    baseY = 350;
                }
                break;
        }

        // 兜底：确保基准位置在屏幕内
        baseX = Math.max(boundary, Math.min(panelWidth - boundary, baseX));
        baseY = Math.max(boundary, Math.min(350, baseY)); // 限制在雷霆战机经典上半区
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
}