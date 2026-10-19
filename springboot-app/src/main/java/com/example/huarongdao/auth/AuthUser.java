package com.example.huarongdao.auth;

/** 放进 HttpSession 的最小用户信息。 */
public record AuthUser(long id, String username) {
}
