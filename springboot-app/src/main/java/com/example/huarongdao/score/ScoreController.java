package com.example.huarongdao.score;

import com.example.huarongdao.auth.ApiException;
import com.example.huarongdao.auth.AuthUser;
import com.example.huarongdao.auth.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 通关成绩与排行榜 REST API。 */
@RestController
@RequestMapping("/api")
public class ScoreController {
    private final ScoreService scoreService;
    private final SessionAuth sessionAuth;

    public ScoreController(ScoreService scoreService, SessionAuth sessionAuth) {
        this.scoreService = scoreService;
        this.sessionAuth = sessionAuth;
    }

    @PostMapping("/scores")
    public ScoreResponse record(@Valid @RequestBody ScoreRequest body, HttpServletRequest request) {
        AuthUser user = sessionAuth.current(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "请先登录。"));
        ScoreResult result = scoreService.record(user.id(), body.preset(), body.moves(), body.seconds());
        return new ScoreResponse(true, result.score(), result.improved());
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardRow> leaderboard() {
        return scoreService.leaderboard();
    }
}
