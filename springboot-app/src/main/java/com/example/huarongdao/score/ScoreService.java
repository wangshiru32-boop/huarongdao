package com.example.huarongdao.score;

import java.util.List;
import org.springframework.stereotype.Service;

/** 记分业务入口。 */
@Service
public class ScoreService {
    private final ScoreRepository scores;

    public ScoreService(ScoreRepository scores) {
        this.scores = scores;
    }

    public ScoreResult record(long userId, int preset, int moves, int seconds) {
        int score = ScoreCalculator.calculate(preset, moves, seconds);
        return new ScoreResult(score, scores.saveIfBetter(userId, preset, moves, seconds, score));
    }

    public List<LeaderboardRow> leaderboard() {
        return scores.top(50);
    }
}
