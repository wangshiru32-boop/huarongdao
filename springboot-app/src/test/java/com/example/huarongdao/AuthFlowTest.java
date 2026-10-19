package com.example.huarongdao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/** 真正启动 Spring Boot 和随机端口，通过 HTTP 验证完整账号与排行榜流程。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:spring-test;DB_CLOSE_DELAY=-1"
})
class AuthFlowTest {
    private static final Pattern CSRF = Pattern.compile("\"csrfToken\":\"([^\"]+)\"");

    @LocalServerPort
    int port;

    @Test
    void registerScoreLeaderboardLogoutAndLoginAgain() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        URI base = URI.create("http://127.0.0.1:" + port + "/huarongdao");

        HttpResponse<String> game = send(client, HttpRequest.newBuilder(base.resolve("/huarongdao/")).GET());
        assertEquals(200, game.statusCode());
        assertTrue(game.body().contains("window.HUARONGDAO_SERVER"));
        assertTrue(game.body().contains("game-bridge.js"));
        String csrf = firstGroup(game.body());

        String registerJson = "{\"username\":\"测试玩家\",\"password\":\"Password123!\",\"confirmPassword\":\"Password123!\"}";
        HttpResponse<String> registered = postJson(client, base.resolve("/huarongdao/api/auth/register"), registerJson, csrf);
        assertEquals(200, registered.statusCode());
        assertTrue(registered.body().contains("\"ok\":true"));
        assertTrue(registered.body().contains("测试玩家"));
        String signedInCsrf = firstGroup(registered.body());

        HttpResponse<String> session = send(client, HttpRequest.newBuilder(base.resolve("/huarongdao/api/auth/session")).GET());
        assertTrue(session.body().contains("\"loggedIn\":true"));
        assertTrue(session.body().contains("测试玩家"));

        String scoreJson = "{\"preset\":0,\"moves\":81,\"seconds\":120}";
        HttpResponse<String> saved = postJson(client, base.resolve("/huarongdao/api/scores"), scoreJson, signedInCsrf);
        assertEquals(200, saved.statusCode());
        assertTrue(saved.body().contains("\"score\":2780"));
        assertTrue(saved.body().contains("\"improved\":true"));

        HttpResponse<String> worse = postJson(client,
                base.resolve("/huarongdao/api/scores"),
                "{\"preset\":0,\"moves\":100,\"seconds\":200}", signedInCsrf);
        assertTrue(worse.body().contains("\"improved\":false"));

        HttpResponse<String> leaderboard = send(client,
                HttpRequest.newBuilder(base.resolve("/huarongdao/api/leaderboard")).GET());
        assertEquals(200, leaderboard.statusCode());
        assertTrue(leaderboard.body().contains("测试玩家"));
        assertTrue(leaderboard.body().contains("2780"));

        HttpResponse<String> loggedOut = postJson(client,
                base.resolve("/huarongdao/api/auth/logout"), "", signedInCsrf);
        assertEquals(200, loggedOut.statusCode());
        String loggedOutCsrf = firstGroup(loggedOut.body());

        String loginJson = "{\"username\":\"测试玩家\",\"password\":\"Password123!\"}";
        HttpResponse<String> loggedIn = postJson(client,
                base.resolve("/huarongdao/api/auth/login"), loginJson, loggedOutCsrf);
        assertEquals(200, loggedIn.statusCode());
        assertTrue(loggedIn.body().contains("\"ok\":true"));

        HttpResponse<String> badCsrf = postJson(client,
                base.resolve("/huarongdao/api/auth/logout"), "", "wrong-token");
        assertEquals(403, badCsrf.statusCode());
    }

    private static HttpResponse<String> postJson(HttpClient client, URI uri, String json, String csrf) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-CSRF-Token", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> send(HttpClient client, HttpRequest.Builder builder) throws Exception {
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String firstGroup(String text) {
        Matcher matcher = CSRF.matcher(text);
        assertTrue(matcher.find(), "响应中没有 csrfToken: " + text);
        return matcher.group(1);
    }
}
