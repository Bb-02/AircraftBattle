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
    // 旧：单列表（兼容迁移用）
    private static List<ScoreRecord> allScores = new ArrayList<>();

    // 新：按难度分榜
    private static java.util.Map<String, List<ScoreRecord>> scoresByDifficulty = new java.util.HashMap<>();

    // 默认难度 key（与 GamePanel 难度枚举对应，未来可扩展）
    private static final String DEFAULT_DIFFICULTY_KEY = "newbie";

    // 得分记录持久化文件（可选，重启游戏后数据不丢失）
    private static final String SCORE_FILE = "scores.dat";

    // 静态代码块：初始化时加载本地得分记录
    static {
        loadScoresFromFile();
    }

    /**
     * 按难度保存得分
     */
    public static void saveScore(String difficultyKey, String nickname, int score) {
        if (difficultyKey == null || difficultyKey.trim().isEmpty()) difficultyKey = DEFAULT_DIFFICULTY_KEY;

        // 1. 空值/非法字符处理（复用原逻辑）
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = "匿名玩家";
        } else {
            nickname = nickname.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_]", "");
            int length = 0;
            StringBuilder sb = new StringBuilder();
            for (char c : nickname.toCharArray()) {
                int charLen = (c >= 0x4E00 && c <= 0x9FA5) ? 2 : 1;
                if (length + charLen > 16) break;
                sb.append(c);
                length += charLen;
            }
            nickname = sb.toString();
            if (nickname.isEmpty()) nickname = "匿名玩家";
        }

        scoresByDifficulty.computeIfAbsent(difficultyKey, k -> new ArrayList<>()).add(new ScoreRecord(nickname, score));
        // 同时写入旧列表（便于兼容旧 UI/调用）
        allScores.add(new ScoreRecord(nickname, score));
        saveScoresToFile();
    }

    /**
     * 兼容旧 API：默认存到 newbie
     */
    public static void saveScore(String nickname, int score) {
        saveScore(DEFAULT_DIFFICULTY_KEY, nickname, score);
    }

    /**
     * 核心方法：获取前5名得分记录（按得分降序）
     * @return 排序后的前5名得分记录列表
     */
    public static List<ScoreRecord> getTopScores(String difficultyKey) {
        if (difficultyKey == null || difficultyKey.trim().isEmpty()) difficultyKey = DEFAULT_DIFFICULTY_KEY;
        List<ScoreRecord> list = scoresByDifficulty.getOrDefault(difficultyKey, new ArrayList<>());
        return list.stream()
                .sorted(Comparator.comparingInt(ScoreRecord::getScore).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 兼容旧 API：默认取 newbie
     */
    public static List<ScoreRecord> getTopScores() {
        return getTopScores(DEFAULT_DIFFICULTY_KEY);
    }

    /**
     * 获取指定难度的历史最高分
     */
    public static int getHighestScore(String difficultyKey) {
        if (difficultyKey == null || difficultyKey.trim().isEmpty()) difficultyKey = DEFAULT_DIFFICULTY_KEY;
        return scoresByDifficulty.getOrDefault(difficultyKey, new ArrayList<>()).stream()
                .mapToInt(ScoreRecord::getScore)
                .max()
                .orElse(0);
    }

    /**
     * 兼容旧 API：默认取 newbie
     */
    public static int getHighestScore() {
        return getHighestScore(DEFAULT_DIFFICULTY_KEY);
    }

    /**
     * 从本地文件加载得分记录（持久化）
     */
    private static void loadScoresFromFile() {
        File file = new File(SCORE_FILE);
        if (!file.exists()) {
            scoresByDifficulty = new java.util.HashMap<>();
            allScores = new ArrayList<>();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();

            if (obj instanceof java.util.Map) {
                // 新格式：difficulty -> list
                //noinspection unchecked
                scoresByDifficulty = (java.util.Map<String, List<ScoreRecord>>) obj;
                if (scoresByDifficulty == null) scoresByDifficulty = new java.util.HashMap<>();

                // 同步 allScores 供旧逻辑兼容（把所有难度的分数汇总）
                allScores = scoresByDifficulty.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());

            } else if (obj instanceof List) {
                // 旧格式：List<ScoreRecord>，自动迁移到 newbie
                //noinspection unchecked
                allScores = (List<ScoreRecord>) obj;
                if (allScores == null) allScores = new ArrayList<>();
                scoresByDifficulty = new java.util.HashMap<>();
                scoresByDifficulty.put(DEFAULT_DIFFICULTY_KEY, new ArrayList<>(allScores));

                // 写回新格式，完成迁移
                saveScoresToFile();
            } else {
                scoresByDifficulty = new java.util.HashMap<>();
                allScores = new ArrayList<>();
            }

        } catch (IOException | ClassNotFoundException e) {
            scoresByDifficulty = new java.util.HashMap<>();
            allScores = new ArrayList<>();
            System.out.println("加载得分记录失败：" + e.getMessage());
        }
    }

    /**
     * 将得分记录保存到本地文件（持久化）
     */
    private static void saveScoresToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
            // 持久化新格式
            oos.writeObject(scoresByDifficulty);
        } catch (IOException e) {
            System.out.println("保存得分记录失败：" + e.getMessage());
        }
    }
}

