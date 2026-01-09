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

    private static final int THRESHOLD = 100; // 每 100 分触发一次

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

    private void onScoreChanged(ScoreChangedEvent evt) {
        try {
            PlayerAircraft player = evt.getPlayer();
            int oldScore = evt.getOldScore();
            int newScore = evt.getNewScore();

            // 如果已经全部满级，就永远不再弹出升级
            if (!hasAnyAvailableUpgrade(player)) return;

            // 简单策略：如果跨过了下一个 THRESHOLD 的倍数，则触发一次
            int oldLevel = oldScore / THRESHOLD;
            int newLevel = newScore / THRESHOLD;
            if (newLevel > oldLevel) {
                int oldCount = appliedCount.getOrDefault(player, 0);
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
