package com.aircraftwar.event.events;

public class FireEvent {
    private final int shooterId;
    private final String bulletType;
    private final int x;
    private final int y;

    public FireEvent(int shooterId, String bulletType, int x, int y) {
        this.shooterId = shooterId;
        this.bulletType = bulletType;
        this.x = x;
        this.y = y;
    }

    public int getShooterId() { return shooterId; }
    public String getBulletType() { return bulletType; }
    public int getX() { return x; }
    public int getY() { return y; }
}

