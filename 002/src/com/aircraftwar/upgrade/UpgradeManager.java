package com.aircraftwar.upgrade;

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

    // 升级触发：首次 300 分；之后门槛逐渐提高（温和二次增长）
    private static final int FIRST_THRESHOLD = 300;
    private static final int MAX_THRESHOLD_SCORE = 10_000; // 期望：1w 分前能把可升级项升满

    private UpgradeManager() {
        EventBus.getDefault().subscribe(ScoreChangedEvent.class, this::onScoreChanged);
    }

    public static UpgradeManager getInstance() { return INSTANCE; }

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
     * 目标：前期不频繁打断，后期更难；但总体不会把升级周期拉太长（1w 分前基本能升满）。
     */
    private int nextUpgradeScore(int appliedTimes) {
        int n = Math.max(0, appliedTimes);

        // 更“后期变难”的二次：score = 300 + 700*n^2
        // n=0 -> 300
        // n=1 -> 1000
        // n=2 -> 3100（略高于 2500，但更符合“稍难一点”的诉求；如需更贴近 2500 可再微调系数）
        int score = FIRST_THRESHOLD + 700 * n * n;

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
        }

        int count = appliedCount.getOrDefault(p, 0);
        appliedCount.put(p, count + 1);
    }
}
