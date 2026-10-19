package com.example.huarongdao.game;

import com.example.huarongdao.auth.AuthUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 读取原游戏 index.html，并注入 Spring Boot 账号配置和桥接脚本。
 *
 * <p>原 index.html 仍是静态游戏的唯一真源，Spring Boot 不复制、不修改它。</p>
 */
@Component
public class GamePageRenderer {
    private final ObjectMapper objectMapper;
    private volatile String gameHtml;

    public GamePageRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String render(String contextPath, AuthUser user, String csrfToken) {
        String html = loadGameHtml();
        String context = contextPath == null ? "" : contextPath;
        String moduleHook = """

                // Spring Boot 专用钩子：游戏逻辑位于 module 中，外部脚本无法直接读取 game。
                const __springCelebrate = celebrate;
                celebrate = function (...args) {
                  const result = __springCelebrate.apply(this, args);
                  if (game.phase === 'won') {
                    window.dispatchEvent(new CustomEvent('huarongdao:victory', {
                      detail: {
                        phase: game.phase,
                        mode: game.mode,
                        preset: game.preset,
                        moves: game.history.length,
                        seconds: Math.max(1, Math.round(game.time / 1000))
                      }
                    }));
                  }
                  return result;
                };
                """;

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("contextPath", context);
        config.put("loggedIn", user != null);
        config.put("username", user == null ? null : user.username());
        config.put("csrfToken", csrfToken);
        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("生成页面配置失败", exception);
        }

        String headInjection = "<link rel=\"stylesheet\" href=\"" + context + "/assets/css/game-account.css\">\n";
        String bodyInjection = "<script>window.HUARONGDAO_SERVER=" + configJson
                + ";</script><script src=\"" + context + "/assets/js/game-bridge.js\" defer></script>\n";
        int headEnd = html.indexOf("</head>");
        int bodyEnd = html.lastIndexOf("</body>");
        int moduleEnd = html.lastIndexOf("</script>");
        if (headEnd < 0 || bodyEnd < 0 || moduleEnd < 0 || moduleEnd > bodyEnd) {
            throw new IllegalStateException("游戏页面结构发生变化，无法注入 Spring Boot 功能");
        }
        return html.substring(0, headEnd) + headInjection
                + html.substring(headEnd, moduleEnd) + moduleHook
                + html.substring(moduleEnd, bodyEnd) + bodyInjection
                + html.substring(bodyEnd);
    }

    private String loadGameHtml() {
        if (gameHtml != null) return gameHtml;
        try (InputStream input = GamePageRenderer.class.getResourceAsStream("/game/index.html")) {
            if (input == null) throw new IllegalStateException("找不到 classpath:/game/index.html");
            gameHtml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return gameHtml;
        } catch (IOException exception) {
            throw new IllegalStateException("读取游戏页面失败", exception);
        }
    }
}
