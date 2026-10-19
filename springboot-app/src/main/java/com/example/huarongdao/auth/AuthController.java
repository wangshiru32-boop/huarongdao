package com.example.huarongdao.auth;

import com.example.huarongdao.user.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 游戏内弹窗使用的认证 REST API。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionAuth sessionAuth;
    private final CsrfService csrfService;

    public AuthController(AuthService authService, SessionAuth sessionAuth, CsrfService csrfService) {
        this.authService = authService;
        this.sessionAuth = sessionAuth;
        this.csrfService = csrfService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest body,
            HttpServletRequest request) {
        if (!body.password().equals(body.confirmPassword())) {
            throw new ApiException(400, "两次输入的密码不一致。");
        }
        UserAccount user = authService.register(body.username(), body.password());
        String csrf = sessionAuth.signIn(request, user);
        return ResponseEntity.ok(new AuthResponse(true, user.username(), csrf));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request) {
        Optional<UserAccount> user = authService.login(body.username(), body.password());
        if (user.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误。");
        }
        String csrf = sessionAuth.signIn(request, user.get());
        return ResponseEntity.ok(new AuthResponse(true, user.get().username(), csrf));
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request) {
        String csrf = sessionAuth.signOut(request);
        return new AuthResponse(true, null, csrf);
    }

    @GetMapping("/session")
    public SessionResponse session(HttpServletRequest request) {
        Optional<AuthUser> user = sessionAuth.current(request);
        String csrf = csrfService.token(request.getSession(true));
        return new SessionResponse(user.isPresent(), user.map(AuthUser::username).orElse(null), csrf);
    }
}
