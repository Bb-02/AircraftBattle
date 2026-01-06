package com.aircraftwar.event.events;

import com.aircraftwar.entity.PlayerAircraft;

public class ScoreChangedEvent {
    private final int oldScore;
    private final int newScore;
    private final PlayerAircraft player;

    public ScoreChangedEvent(int oldScore, int newScore, PlayerAircraft player) {
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.player = player;
    }

    public int getOldScore() { return oldScore; }
    public int getNewScore() { return newScore; }
    public PlayerAircraft getPlayer() { return player; }
}
