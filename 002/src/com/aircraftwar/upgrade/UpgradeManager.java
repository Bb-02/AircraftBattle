package com.aircraftwar.upgrade;

import com.aircraftwar.entity.DifficultyProfile;
import com.aircraftwar.entity.PlayerAircraft;
import com.aircraftwar.event.EventBus;
import com.aircraftwar.event.events.ScoreChangedEvent;
import com.aircraftwar.event.events.UpgradeRequestEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * UpgradeManager：订阅分数变化并在阈值处发布升级请求事件。
 * 规则：只要存在至少一个未满级的升级项，就允许继续触发升级弹窗。
 */
public class UpgradeManager {
    private static final UpgradeManager INSTANCE = new UpgradeManager();
    private final Map<PlayerAircraft, Integer> appliedCount = new HashMap<>();

    // 当前局的难度（用于升级门槛倍率）
    private volatile DifficultyProfile.DifficultyKey difficulty = DifficultyProfile.DifficultyKey.NEWBIE;

    // 升级触发：改为“渐进式手工曲线”（不再使用二次函数）
    // 要求：
    //  - 第1次升级：800
    //  - 第2次升级：2000
    //  - 第3次升级：3500 左右
    //  - 之后逐渐增加，并在 15000 左右封顶
    private static final int TARGET_FULL_UPGRADE_SCORE = 15_000;
    private static final int MAX_THRESHOLD_SCORE = TARGET_FULL_UPGRADE_SCORE;

    // 基础门槛表：index=已应用升级次数 appliedTimes
    // appliedTimes=0 -> 800（第一次升级）
    // appliedTimes=1 -> 2000（第二次升级）
    // appliedTimes=2 -> 3500（第三次升级）
    // 之后继续递增（可按手感微调）
    private static final int[] THRESHOLDS = new int[] {
            800,
            2000,
            3500,
            5200,
            7100,
            9300,
            11800,
            14500
    };

    private UpgradeManager() {
        EventBus.getDefault().subscribe(ScoreChangedEvent.class, this::onScoreChanged);
    }

    public static UpgradeManager getInstance() { return INSTANCE; }

    /**
     * 每局开局/难度切换时调用，用于让升级门槛随难度变化。
     */
    public void setDifficulty(DifficultyProfile.DifficultyKey difficulty) {
        this.difficulty = (difficulty == null) ? DifficultyProfile.DifficultyKey.NEWBIE : difficulty;
    }

    /**
     * 开新一局时建议调用，避免 PlayerAircraft 对象复用导致 appliedCount 延续。
     */
    public void resetForPlayer(PlayerAircraft p) {
        if (p != null) appliedCount.remove(p);
    }

    /**
     * 是否该玩家该升级项已满级
     */
    public boolean isMaxed(PlayerAircraft p, UpgradeOption opt) {
        if (p == null || opt == null) return true;
        switch (opt) {
            case FIRE:
                return p.getFireLevel() >= p.getMaxFireLevel();
            case SPEED:
                return p.getSpeedLevel() >= p.getMaxSpeedLevel();
            case FIRE_RATE:
                return p.getFireRateLevel() >= p.getMaxFireRateLevel();
            default:
                return true;
        }
    }

    /**
     * 是否仍然存在至少一个可升级项
     */
    public boolean hasAnyAvailableUpgrade(PlayerAircraft p) {
        for (UpgradeOption opt : UpgradeOption.values()) {
            if (!isMaxed(p, opt)) return true;
        }
        return false;
    }

    /**
     * 计算“下一次升级弹窗”需要达到的分数。
     */
    private int nextUpgradeScore(int appliedTimes) {
        int n = Math.max(0, appliedTimes);

        // 渐进式门槛：前几次用表，之后按“最后一段增量”继续推进，并在 15000 封顶
        int base;
        if (n < THRESHOLDS.length) {
            base = THRESHOLDS[n];
        } else {
            int last = THRESHOLDS[THRESHOLDS.length - 1];
            int prev = THRESHOLDS[THRESHOLDS.length - 2];
            int delta = Math.max(800, last - prev); // 兜底增量，避免后期增长太慢
            base = last + (n - (THRESHOLDS.length - 1)) * delta;
        }

        // 难度倍率：不可能/老手更难升级
        double mult = DifficultyProfile.upgradeThresholdMultiplier(this.difficulty);
        int score = (int) Math.round(base * mult);

        return Math.min(score, MAX_THRESHOLD_SCORE);
    }

    private void onScoreChanged(ScoreChangedEvent evt) {
        try {
            PlayerAircraft player = evt.getPlayer();
            int oldScore = evt.getOldScore();
            int newScore = evt.getNewScore();

            if (!hasAnyAvailableUpgrade(player)) return;

            int oldCount = appliedCount.getOrDefault(player, 0);
            int threshold = nextUpgradeScore(oldCount);

            // 只要从旧分跨过“下一次门槛”就触发
            if (oldScore < threshold && newScore >= threshold) {
                EventBus.getDefault().post(new UpgradeRequestEvent(player, oldCount));
            }
        } catch (Exception ignored) {}
    }

    public int getAppliedCount(PlayerAircraft p) {
        return appliedCount.getOrDefault(p, 0);
    }

    public void applyUpgrade(PlayerAircraft p, UpgradeOption opt) {
        if (p == null || opt == null) return;

        // 如果该项已满级：不做事，也不计数
        if (isMaxed(p, opt)) return;

        // 根据选项调用玩家暴露的 API
        switch (opt) {
            case FIRE:
                p.increaseFirePower();
                break;
            case SPEED:
                p.increaseSpeed();
                break;
            case FIRE_RATE:
                p.increaseFireRate();
                break;
        }

        int count = appliedCount.getOrDefault(p, 0);
        appliedCount.put(p, count + 1);
    }
}
