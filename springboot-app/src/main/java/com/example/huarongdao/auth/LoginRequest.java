package com.example.huarongdao.auth;

import jakarta.validation.constraints.NotBlank;

/** 登录 API 请求体。 */
public record LoginRequest(
        @NotBlank(message = "请输入用户名。") String username,
        @NotBlank(message = "请输入密码。") String password) {
}
