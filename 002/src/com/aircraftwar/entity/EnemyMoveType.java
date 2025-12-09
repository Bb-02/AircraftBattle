package com.aircraftwar.entity;

/**
 * 敌机运动类型（新增雷霆战机风格）
 */
public enum EnemyMoveType {
    // 基础类型
    HOVER,               // 悬停（定点巡航）
    LEFT_RIGHT,          // 左右来回
    // 雷霆战机风格类型
    DIAGONAL_BACK_FORTH, // 斜向来回（45度）
    SPIRAL,              // 螺旋移动
    SWAY_FORWARD,        // 左右摇摆+缓慢前进
    ZIGZAG               // Z型折线移动
}