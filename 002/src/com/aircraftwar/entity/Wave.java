package com.aircraftwar.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 波次管理类（关联小队，无尽型）
 */
public class Wave {
    private int waveNumber;          // 波次编号（无限递增）
    private List<EnemySquad> squads; // 本波小队列表
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
            squadCount += 2;
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
            return;
        }

        // 所有小队全灭：标记波次结束
        boolean allSquadsDead = squads.stream().allMatch(EnemySquad::isAllDead);
        if (allSquadsDead && !isWaveOver) {
            isWaveOver = true;
        }
    }

    // 更新本波所有小队
    public void updateWave() {
        // 检查小队生成
        for (EnemySquad squad : squads) {
            squad.checkSpawn(startTime);
            // 移动小队
            squad.moveSquad();
            // 更新小队子弹
            squad.updateSquadBullets();
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
        return allEnemies;
    }

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
