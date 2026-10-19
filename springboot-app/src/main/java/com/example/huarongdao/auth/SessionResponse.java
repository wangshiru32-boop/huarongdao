package com.example.huarongdao.auth;

/** 页面刷新时可用来重新读取登录态。 */
public record SessionResponse(boolean loggedIn, String username, String csrfToken) {
}
