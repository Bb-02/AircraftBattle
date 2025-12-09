package com.aircraftwar.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分数排行榜工具类（支持昵称+分数持久化）
 */
public class ScoreUtil {
    // 分数存储文件路径（用户主目录下的aircraftwar_scores.txt）
    private static final String SCORE_FILE_PATH =
            System.getProperty("user.home") + File.separator + "aircraftwar_scores.txt";
    // 排行榜最大记录数
    private static final int MAX_RECORDS = 10;

    /**
     * 保存昵称+分数到排行榜
     * @param nickname 玩家昵称
     * @param score 玩家分数
     */
    public static void saveScore(String nickname, int score) {
        // 获取现有排行榜
        List<ScoreRecord> records = getScoreRecordList();
        // 添加新记录
        records.add(new ScoreRecord(nickname, score));
        // 排序（降序）并截取前10条
        Collections.sort(records);
        if (records.size() > MAX_RECORDS) {
            records = records.subList(0, MAX_RECORDS);
        }
        // 写入文件（格式：昵称,分数）
        try (PrintWriter writer = new PrintWriter(new FileWriter(SCORE_FILE_PATH))) {
            for (ScoreRecord record : records) {
                writer.println(record.getNickname() + "," + record.getScore());
            }
        } catch (IOException e) {
            System.err.println("保存分数失败：" + e.getMessage());
        }
    }

    /**
     * 获取排行榜记录列表（包含昵称+分数）
     * @return 按分数降序排列的记录列表
     */
    public static List<ScoreRecord> getScoreRecordList() {
        List<ScoreRecord> records = new ArrayList<>();
        File scoreFile = new File(SCORE_FILE_PATH);

        // 如果文件不存在，返回空列表
        if (!scoreFile.exists()) {
            return records;
        }

        // 读取文件中的记录
        try (BufferedReader reader = new BufferedReader(new FileReader(scoreFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 兼容旧格式（纯分数）和新格式（昵称,分数）
                String[] parts = line.split(",");
                String nickname;
                int score;
                if (parts.length == 2) {
                    // 新格式：昵称,分数
                    nickname = parts[0].trim();
                    try {
                        score = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        continue; // 跳过无效分数行
                    }
                } else {
                    // 旧格式：纯分数，补默认昵称
                    try {
                        score = Integer.parseInt(line.trim());
                        nickname = "Anonymous";
                    } catch (NumberFormatException e) {
                        continue; // 跳过无效行
                    }
                }
                records.add(new ScoreRecord(nickname, score));
            }
            // 排序（降序）
            Collections.sort(records);
        } catch (IOException e) {
            System.err.println("读取排行榜失败：" + e.getMessage());
        }
        return records;
    }

    /**
     * 获取历史最高分
     * @return 历史最高分，无记录则返回0
     */
    public static int getHighestScore() {
        List<ScoreRecord> records = getScoreRecordList();
        if (records.isEmpty()) {
            return 0;
        }
        return records.get(0).getScore();
    }
}