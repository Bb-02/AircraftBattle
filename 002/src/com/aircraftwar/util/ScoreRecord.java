package com.aircraftwar.util;

/**
 * 分数记录实体类（包含玩家昵称和分数）
 */
public class ScoreRecord implements Comparable<ScoreRecord> {
    private String nickname; // 玩家昵称
    private int score;       // 分数

    public ScoreRecord(String nickname, int score) {
        // 处理空昵称，使用默认值
        this.nickname = (nickname == null || nickname.trim().isEmpty())
                ? "Player_" + (int)(Math.random() * 1000)
                : nickname.trim();
        this.score = score;
    }

    // 按分数降序排序，分数相同则按昵称升序
    @Override
    public int compareTo(ScoreRecord o) {
        if (this.score != o.score) {
            return Integer.compare(o.score, this.score); // 降序
        }
        return this.nickname.compareTo(o.nickname); // 升序
    }

    // Getter
    public String getNickname() {
        return nickname;
    }

    public int getScore() {
        return score;
    }
}