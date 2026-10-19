package com.example.huarongdao.auth;

import com.example.huarongdao.user.UserAccount;
import com.example.huarongdao.user.UserRepository;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** 注册、登录的业务规则。 */
@Service
public class AuthService {
    private static final Pattern USERNAME = Pattern.compile("^[\\p{L}\\p{N}_]{3,20}$");
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String username, String password) {
        String clean = username == null ? "" : username.trim();
        if (!USERNAME.matcher(clean).matches()) {
            throw new ApiException(400, "用户名需为 3–20 个汉字、字母、数字或下划线。");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new ApiException(400, "密码需要 8–72 个字符。");
        }
        if (users.findByUsername(clean).isPresent()) {
            throw new ApiException(409, "这个用户名已经被注册。");
        }
        try {
            return users.create(clean, passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(409, "这个用户名已经被注册。");
        }
    }

    public Optional<UserAccount> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        String clean = username.trim();
        if (!USERNAME.matcher(clean).matches()) return Optional.empty();
        Optional<UserAccount> user = users.findByUsername(clean);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().passwordHash())) {
            return Optional.empty();
        }
        return user;
    }
}
