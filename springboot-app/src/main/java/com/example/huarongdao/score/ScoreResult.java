package com.example.huarongdao.score;

/** improved=false 表示本次没有超过该阵型原来的最高分。 */
public record ScoreResult(int score, boolean improved) {
}
