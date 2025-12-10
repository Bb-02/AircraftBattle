package com.aircraftwar.util;

import com.aircraftwar.entity.ScoreRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 得分工具类（补全getTopScores()，实现前5名排行榜+持久化）
 */
public class ScoreUtil {
    // 静态列表存储所有得分记录（内存中）
    private static List<ScoreRecord> allScores = new ArrayList<>();
    // 得分记录持久化文件（可选，重启游戏后数据不丢失）
    private static final String SCORE_FILE = "scores.dat";

    // 静态代码块：初始化时加载本地得分记录
    static {
        loadScoresFromFile();
    }

    /**
     * 保存玩家得分到列表+本地文件
     * @param nickname 玩家昵称
     * @param score 玩家得分
     */
    public static void saveScore(String nickname, int score) {
        // 1. 空值处理
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "匿名玩家";
        } else {
            // 2. 过滤非法字符
            nickname = nickname.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_]", "");
            // 3. 数据层二次长度限制（和输入框规则一致）
            int length = 0;
            StringBuilder sb = new StringBuilder();
            for (char c : nickname.toCharArray()) {
                int charLen = (c >= 0x4E00 && c <= 0x9FA5) ? 2 : 1;
                if (length + charLen > 16) {
                    break;
                }
                sb.append(c);
                length += charLen;
            }
            nickname = sb.toString();
            // 4. 截断后仍为空则设为匿名
            if (nickname.isEmpty()) {
                nickname = "匿名玩家";
            }
        }
        allScores.add(new ScoreRecord(nickname, score));
        saveScoresToFile();
    }

    /**
     * 核心方法：获取前5名得分记录（按得分降序）
     * @return 排序后的前5名得分记录列表
     */
    public static List<ScoreRecord> getTopScores() {
        // 按得分降序排序，仅保留前5条
        return allScores.stream()
                .sorted(Comparator.comparingInt(ScoreRecord::getScore).reversed()) // 降序排序
                .limit(5) // 限制只返回前5名
                .collect(Collectors.toList());
    }

    /**
     * 获取历史最高分
     * @return 历史最高得分（无记录时返回0）
     */
    public static int getHighestScore() {
        return allScores.stream()
                .mapToInt(ScoreRecord::getScore)
                .max()
                .orElse(0);
    }

    /**
     * 从本地文件加载得分记录（持久化）
     */
    private static void loadScoresFromFile() {
        File file = new File(SCORE_FILE);
        if (!file.exists()) {
            return; // 文件不存在则返回空列表
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            allScores = (List<ScoreRecord>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // 加载失败时清空列表，避免游戏崩溃
            allScores = new ArrayList<>();
            System.out.println("加载得分记录失败：" + e.getMessage());
        }
    }

    /**
     * 将得分记录保存到本地文件（持久化）
     */
    private static void saveScoresToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
            oos.writeObject(allScores);
        } catch (IOException e) {
            System.out.println("保存得分记录失败：" + e.getMessage());
        }
    }
}