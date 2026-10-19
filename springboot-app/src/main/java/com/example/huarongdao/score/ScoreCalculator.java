package com.example.huarongdao.score;

/** 服务端积分规则：5000 + 阵型奖励 - 步数×20 - 秒数×5，最低 100。 */
public final class ScoreCalculator {
    private ScoreCalculator() {
    }

    public static int calculate(int preset, int moves, int seconds) {
        if (preset < 0 || preset > 7 || moves <= 0 || moves > 2000 || seconds <= 0 || seconds > 86400) {
            throw new IllegalArgumentException("成绩参数不合法。");
        }
        return Math.max(100, 5000 + preset * 80 - moves * 20 - seconds * 5);
    }
}
