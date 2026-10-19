package com.example.huarongdao.game;

import com.example.huarongdao.auth.AuthUser;
import com.example.huarongdao.auth.CsrfService;
import com.example.huarongdao.auth.SessionAuth;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** 游戏入口：返回原 index.html 的 Spring Boot 注入版本。 */
@Controller
public class GamePageController {
    private final GamePageRenderer renderer;
    private final SessionAuth sessionAuth;
    private final CsrfService csrfService;

    public GamePageController(GamePageRenderer renderer, SessionAuth sessionAuth, CsrfService csrfService) {
        this.renderer = renderer;
        this.sessionAuth = sessionAuth;
        this.csrfService = csrfService;
    }

    @GetMapping({"/", "/index.html"})
    @ResponseBody
    public ResponseEntity<String> game(HttpServletRequest request) {
        Optional<AuthUser> user = sessionAuth.current(request);
        String csrf = csrfService.token(request.getSession(true));
        String html = renderer.render(request.getContextPath(), user.orElse(null), csrf);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore())
                .body(html);
    }
}
