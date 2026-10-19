package com.example.huarongdao.auth;

/** 登录、注册、退出成功后的统一响应。 */
public record AuthResponse(boolean ok, String username, String csrfToken) {
}
