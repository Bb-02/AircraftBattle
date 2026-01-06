package com.aircraftwar.upgrade;

import com.aircraftwar.entity.PlayerAircraft;
import com.aircraftwar.event.EventBus;
import com.aircraftwar.event.events.ScoreChangedEvent;
import com.aircraftwar.event.events.UpgradeRequestEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单的 UpgradeManager skeleton：订阅分数变化并在阈值处发布升级请求事件。
 * 目前使用固定阈值：每达到 100 分触发一次升级，最多触发 3 次（每个玩家）。
 */
public class UpgradeManager {
    private static final UpgradeManager INSTANCE = new UpgradeManager();
    private final Map<PlayerAircraft, Integer> appliedCount = new HashMap<>();
    private final int THRESHOLD = 100; // 每 100 分触发一次
    private final int MAX_UPGRADES = 3;

    private UpgradeManager() {
        EventBus.getDefault().subscribe(ScoreChangedEvent.class, this::onScoreChanged);
    }

    public static UpgradeManager getInstance() { return INSTANCE; }

    private void onScoreChanged(ScoreChangedEvent evt) {
        try {
            PlayerAircraft player = evt.getPlayer();
            int oldScore = evt.getOldScore();
            int newScore = evt.getNewScore();
            int oldCount = appliedCount.getOrDefault(player, 0);
            if (oldCount >= MAX_UPGRADES) return;
            // 简单策略：如果跨过了下一个 THRESHOLD 的倍数，则触发一次
            int oldLevel = oldScore / THRESHOLD;
            int newLevel = newScore / THRESHOLD;
            if (newLevel > oldLevel) {
                // 触发升级请求（UI 层订阅并弹出选择）
                EventBus.getDefault().post(new UpgradeRequestEvent(player, oldCount));
            }
        } catch (Exception ignored) {}
    }

    public int getAppliedCount(PlayerAircraft p) {
        return appliedCount.getOrDefault(p, 0);
    }

    public void applyUpgrade(PlayerAircraft p, UpgradeOption opt) {
        int count = appliedCount.getOrDefault(p, 0);
        if (count >= MAX_UPGRADES) return;
        // 根据选项调用玩家暴露的 API
        switch (opt) {
            case FIRE:
                p.increaseFirePower();
                break;
            case SHIELD:
                p.addShield();
                break;
            case SPEED:
                p.increaseSpeed();
                break;
        }
        appliedCount.put(p, count + 1);
    }
}

