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

    // 构造方法（无尽型，波次难度递增）
    public Wave(int waveNumber) {
        this.waveNumber = waveNumber;
        this.startTime = System.currentTimeMillis();
        // 波次越高，时长越短（最低15秒）
        this.duration = 40000 - (waveNumber * 1000);
        if (duration < 15000) duration = 15000;
        this.isWaveOver = false;

        // 生成本波小队列表（波次越高，小队数越多）
        int squadCount = 2 + (waveNumber / 3);
        if (waveNumber <= 1) squadCount = 3; // 开局更紧凑一点
        if (squadCount > 6) squadCount = 6;
        this.squads = new ArrayList<>();

        // 小队按时间间隔生成（开局更快，后续保持随机）
        long spawnDelay = 0;
        for (int i = 0; i < squadCount; i++) {
            if (waveNumber <= 1) {
                spawnDelay += (1100 + (long) (Math.random() * 1000)); // 1.1-2.1s
            } else {
                spawnDelay += (1700 + (long) (Math.random() * 2400)); // 1.7-4.1s（略快于原来的2-5s）
            }
            squads.add(new EnemySquad(i + 1, waveNumber, spawnDelay));
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
}