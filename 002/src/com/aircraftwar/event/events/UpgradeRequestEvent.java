package com.aircraftwar.event.events;

import com.aircraftwar.entity.PlayerAircraft;

public class UpgradeRequestEvent {
    private final PlayerAircraft player;
    private final int appliedCount;

    public UpgradeRequestEvent(PlayerAircraft player, int appliedCount) {
        this.player = player;
        this.appliedCount = appliedCount;
    }

    public PlayerAircraft getPlayer() { return player; }
    public int getAppliedCount() { return appliedCount; }
}
