package com.example.huarongdao.auth;

import com.example.huarongdao.user.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 登录 Session 的统一读写入口。 */
@Component
public class SessionAuth {
    private static final String USER_KEY = "authUser";
    private final CsrfService csrf;

    public SessionAuth(CsrfService csrf) {
        this.csrf = csrf;
    }

    public Optional<AuthUser> current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(USER_KEY) instanceof AuthUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /** 登录后销毁旧 Session 并创建新 Session，避免 Session Fixation。 */
    public String signIn(HttpServletRequest request, UserAccount user) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_KEY, new AuthUser(user.id(), user.username()));
        return csrf.token(session);
    }

    /** 退出时同样轮换 Session，并返回新的 CSRF Token，方便页面继续登录。 */
    public String signOut(HttpServletRequest request) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        return csrf.token(request.getSession(true));
    }
}
