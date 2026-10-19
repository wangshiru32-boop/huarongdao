package com.example.huarongdao.score;

/** 排行榜一行：总积分 = 各经典阵型最高分之和。 */
public record LeaderboardRow(
        String username,
        int totalPoints,
        int completedPresets,
        int bestScore) {
}
