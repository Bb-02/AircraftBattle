package com.aircraftwar.entity;

import java.awt.*;

public interface IBullet {
    void update();
    void render(Graphics g);
    Rectangle getCollisionRect();
    boolean isAlive();
    void setAlive(boolean alive);
    int getDamage();
    String getType();
    int getOwnerId();
}
