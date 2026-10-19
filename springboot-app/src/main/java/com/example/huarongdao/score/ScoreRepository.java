package com.example.huarongdao.score;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** 成绩表 JDBC 仓库。 */
@Repository
public class ScoreRepository {
    private final JdbcTemplate jdbc;

    public ScoreRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public boolean saveIfBetter(long userId, int preset, int moves, int seconds, int score) {
        List<Integer> current = jdbc.query(
                "SELECT score FROM scores WHERE user_id = ? AND preset_index = ? FOR UPDATE",
                (result, row) -> result.getInt(1), userId, preset);
        if (current.isEmpty()) {
            try {
                jdbc.update("""
                        INSERT INTO scores (user_id, preset_index, moves, duration_seconds, score)
                        VALUES (?, ?, ?, ?, ?)
                        """, userId, preset, moves, seconds, score);
                return true;
            } catch (DuplicateKeyException race) {
                current = List.of(0);
            }
        }
        if (score > current.get(0)) {
            jdbc.update("""
                    UPDATE scores
                    SET moves = ?, duration_seconds = ?, score = ?, achieved_at = CURRENT_TIMESTAMP
                    WHERE user_id = ? AND preset_index = ?
                    """, moves, seconds, score, userId, preset);
            return true;
        }
        return false;
    }

    public List<LeaderboardRow> top(int limit) {
        return jdbc.query("""
                SELECT u.username,
                       COALESCE(SUM(s.score), 0) AS total_points,
                       COUNT(s.id) AS completed_presets,
                       COALESCE(MAX(s.score), 0) AS best_score
                FROM users u
                JOIN scores s ON s.user_id = u.id
                GROUP BY u.id, u.username, u.created_at
                ORDER BY total_points DESC, best_score DESC, u.created_at ASC
                LIMIT ?
                """, (result, row) -> new LeaderboardRow(
                        result.getString("username"),
                        result.getInt("total_points"),
                        result.getInt("completed_presets"),
                        result.getInt("best_score")), limit);
    }
}
