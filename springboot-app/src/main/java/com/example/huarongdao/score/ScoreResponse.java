package com.example.huarongdao.score;

/** 成绩 API 响应。 */
public record ScoreResponse(boolean ok, int score, boolean improved) {
}
