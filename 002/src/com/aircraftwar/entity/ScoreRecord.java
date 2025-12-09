// 包名必须是 com.aircraftwar.entity，与文件路径一致
package com.aircraftwar.entity;

// 实现 Serializable 接口（支持 ScoreUtil 的文件持久化）
import java.io.Serializable;

/**
 * 得分记录实体类（必须放在 com.aircraftwar.entity 包下）
 */
public class ScoreRecord implements Serializable {
    // 序列化版本号（固定值，避免序列化/反序列化异常）
    private static final long serialVersionUID = 1L;

    // 玩家昵称
    private String nickname;
    // 玩家得分
    private int score;

    // 构造方法（必须有，ScoreUtil 中会调用）
    public ScoreRecord(String nickname, int score) {
        this.nickname = nickname;
        this.score = score;
    }

    // Getter 方法（ScoreUtil、GamePanel 会调用）
    public String getNickname() {
        return nickname;
    }

    public int getScore() {
        return score;
    }

    // Setter 方法（可选，备用）
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setScore(int score) {
        this.score = score;
    }
}