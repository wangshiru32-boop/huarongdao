package com.example.huarongdao.auth;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

/** 生成和校验 Session CSRF Token。 */
@Service
public class CsrfService {
    private static final String KEY = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String token(HttpSession session) {
        Object existing = session.getAttribute(KEY);
        if (existing instanceof String token && !token.isBlank()) return token;
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(KEY, token);
        return token;
    }

    public boolean matches(HttpSession session, String submitted) {
        if (session == null || submitted == null) return false;
        Object expected = session.getAttribute(KEY);
        if (!(expected instanceof String expectedToken)) return false;
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }
}
