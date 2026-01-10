package com.aircraftwar.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 波次管理类（关联小队，无尽型）
 */
public class Wave {
    private int waveNumber;          // 波次编号（无限递增）
    private List<EnemySquad> squads; // 本波小队列表

    // ===== 独立敌人：Bee（不属于小队，更像“独立怪”） =====
    private final List<BeeAircraft> independentBees = new ArrayList<>();
    private long nextBeeSpawnAtMs = 0L;
    private int spawnedBeeCount = 0;

    private long startTime;          // 本波开始时间
    private long duration;           // 本波时长（随波次递减，最低15秒）
    private boolean isWaveOver;      // 本波是否结束
    private com.aircraftwar.entity.DifficultyProfile.DifficultyKey difficulty;

    // 构造方法（无尽型，波次难度递增）
    // 兼容旧构造：默认新手
    public Wave(int waveNumber) {
        this(waveNumber, com.aircraftwar.entity.DifficultyProfile.DifficultyKey.NEWBIE);
    }

    // 新构造：带难度
    public Wave(int waveNumber, com.aircraftwar.entity.DifficultyProfile.DifficultyKey difficulty) {
        this.waveNumber = waveNumber;
        this.difficulty = (difficulty == null) ? com.aircraftwar.entity.DifficultyProfile.DifficultyKey.NEWBIE : difficulty;
        this.startTime = System.currentTimeMillis();
        this.nextBeeSpawnAtMs = this.startTime + 1200; // 开局延后一点，避免一开始就太挤
        this.spawnedBeeCount = 0;
        // 波次越高，时长越短（最低15秒）
        this.duration = 40000 - (waveNumber * 1000);
        if (duration < 15000) duration = 15000;
        this.isWaveOver = false;

        // 生成本波小队列表（波次越高，小队数越多）
        int squadCount = 3 + (waveNumber / 3);
        if (waveNumber <= 1) squadCount = 4; // 开局更紧凑
        if (waveNumber <= 3 && squadCount < 5) squadCount = 5; // 1~3 波适当加压

        // 难度额外加压：不可能 -> 每波多 1~2 个小队（上限也更高）
        int maxSquads = 7;
        if (this.difficulty == com.aircraftwar.entity.DifficultyProfile.DifficultyKey.IMPOSSIBLE) {
            // ✅ 微调：开局（前2波）少一点，避免刚开始就太挤；后期仍然保持压迫感
            if (waveNumber <= 2) {
                squadCount += 1;
            } else {
                squadCount += 2;
            }
            maxSquads = 9;
        }
        if (squadCount > maxSquads) squadCount = maxSquads;

        this.squads = new ArrayList<>();

        // 小队按时间间隔生成：不可能更密集
        double spawnMult = 1.0;
        if (this.difficulty == com.aircraftwar.entity.DifficultyProfile.DifficultyKey.IMPOSSIBLE) {
            spawnMult = 0.82; // 生成更快、更密
        }

        long spawnDelay = 0;
        for (int i = 0; i < squadCount; i++) {
            long add;
            if (waveNumber <= 1) {
                // 0.8-1.4s：开局更有压迫感
                add = (1000 + (long) (Math.random() * 600));
            } else if (waveNumber <= 3) {
                // 1.0-2.0s：前几波更密集
                add = (1000 + (long) (Math.random() * 1000));
            } else {
                // 随波次稍微缩短生成间隔（下限兜底），避免后期变“空窗期”
                long base = Math.max(900, 1700 - waveNumber * 60L);
                long jitter = Math.max(700, 2200 - waveNumber * 40L);
                add = (base + (long) (Math.random() * jitter));
            }

            spawnDelay += (long) Math.max(250, Math.round(add * spawnMult));
            squads.add(new EnemySquad(i + 1, waveNumber, spawnDelay, this.difficulty));
        }
    }

    // ===== Bee 独立生成逻辑 =====
    private int getBeeMaxPerWave() {
        // 每波最多多少只独立 Bee。随难度略升，随波次略升但有上限
        int base = 1 + Math.min(2, waveNumber / 4); // 1~3
        if (difficulty == com.aircraftwar.entity.DifficultyProfile.DifficultyKey.VETERAN) base += 1;
        if (difficulty == com.aircraftwar.entity.DifficultyProfile.DifficultyKey.IMPOSSIBLE) base += 2;
        return Math.min(base, 6);
    }

    private long nextBeeIntervalMs() {
        // 越难 -> 越快；波次越高 -> 稍快，但不要太夸张
        long base;
        if (difficulty == com.aircraftwar.entity.DifficultyProfile.DifficultyKey.IMPOSSIBLE) {
            base = 1600 + (long) (Math.random() * 900);   // 1.6~2.5s
        } else if (difficulty == com.aircraftwar.entity.DifficultyProfile.DifficultyKey.VETERAN) {
            base = 2100 + (long) (Math.random() * 1000);  // 2.1~3.1s
        } else {
            base = 2800 + (long) (Math.random() * 1200);  // 2.8~4.0s
        }
        long waveTrim = Math.min(900, waveNumber * 60L);
        return Math.max(900, base - waveTrim);
    }

    private void trySpawnIndependentBee(long now) {
        if (isWaveOver) return;
        if (spawnedBeeCount >= getBeeMaxPerWave()) return;
        if (now < nextBeeSpawnAtMs) return;

        int w = com.aircraftwar.util.GameConfig.SCREEN_WIDTH;

        // 出场：更像敌机小队那种“突然进入战斗区域”的感觉
        // 直接生成在屏幕上方可见范围（靠近小队 ENTER 目标高度），避免屏幕外开火/等待太久
        int initX = com.aircraftwar.util.GameConfig.BOUNDARY_PADDING + (int) (Math.random() * (w - com.aircraftwar.util.GameConfig.BOUNDARY_PADDING * 2));
        int initY = 70 + (int) (Math.random() * 40); // 70~109

        BeeAircraft bee = new BeeAircraft(waveNumber, initX, initY, difficulty);
        // Bee 自己 move() 会平滑运动并射击；这里仅纳入管理
        independentBees.add(bee);
        spawnedBeeCount++;

        nextBeeSpawnAtMs = now + nextBeeIntervalMs();
    }

    // 检查本波是否结束（所有小队全灭 OR 时间结束）
    public void checkWaveOver() {
        // 时间结束：标记波次结束
        if (System.currentTimeMillis() - startTime >= duration && !isWaveOver) {
            isWaveOver = true;
            // 剩余敌机逃跑
            for (EnemySquad squad : squads) {
                if (squad.isSpawned() && !squad.isAllDead()) {
                    squad.getEnemies().forEach(enemy -> {
                        if (enemy.isAlive()) enemy.setEscaping(true);
                    });
                }
            }

            // Bee 也开始向上退场
            for (BeeAircraft bee : independentBees) {
                if (bee != null && bee.isAlive()) {
                    bee.startEscapeUp();
                }
            }
            return;
        }

        // 所有小队全灭：标记波次结束
        boolean allSquadsDead = squads.stream().allMatch(EnemySquad::isAllDead);
        // 独立 Bee 也需要全部消失，才算“清场结束”
        boolean allBeesGone = independentBees.stream().noneMatch(b -> b != null && b.isAlive());
        if (allSquadsDead && allBeesGone && !isWaveOver) {
            isWaveOver = true;
        }
    }

    // 更新本波所有小队
    public void updateWave() {
        long now = System.currentTimeMillis();

        // 检查小队生成
        for (EnemySquad squad : squads) {
            squad.checkSpawn(startTime);
            // 移动小队
            squad.moveSquad();
            // 更新小队子弹
            squad.updateSquadBullets();
        }

        // 独立 Bee：按难度/波次调度生成，并更新移动与子弹
        trySpawnIndependentBee(now);
        // 注意：Bee 死亡后仍要让残留子弹继续飞行直到越界，所以不能立刻移除 Bee 对象。
        // 这里的策略：
        // - Bee 活着：正常 move + updateBullets
        // - Bee 死亡：只 updateBullets；当 bullets 清空后才移除该 Bee
        independentBees.removeIf(b -> {
            if (b == null) return true;
            if (b.isAlive()) return false;
            // 已死亡：若没有任何残留子弹，则可以移除
            List<IBullet> bs = b.getBullets();
            return bs == null || bs.isEmpty();
        });
        for (BeeAircraft bee : independentBees) {
            if (bee.isAlive()) {
                bee.move();
            }
            bee.updateBullets();
        }

        // 检查波次是否结束
        checkWaveOver();
    }

    // 获取本波所有敌机（用于碰撞检测）
    public List<EnemyAircraft> getAllEnemies() {
        List<EnemyAircraft> allEnemies = new ArrayList<>();
        for (EnemySquad squad : squads) {
            if (squad.isSpawned()) {
                allEnemies.addAll(squad.getEnemies());
            }
        }

        // Bee 作为 EnemyAircraft 子类，也加入碰撞检测
        allEnemies.addAll(independentBees);
        return allEnemies;
    }

    // 供 UI/调试：独立 Bee 列表
    public List<BeeAircraft> getIndependentBees() { return independentBees; }

    // Getter & Setter
    public int getWaveNumber() { return waveNumber; }
    public List<EnemySquad> getSquads() { return squads; }
    public boolean isWaveOver() { return isWaveOver; }
    public long getDuration() { return duration; }
    public long getStartTime() { return startTime; }
    public com.aircraftwar.entity.DifficultyProfile.DifficultyKey getDifficulty() {
        return difficulty;
    }
}
