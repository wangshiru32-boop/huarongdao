package com.example.huarongdao.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 注册 API 请求体。 */
public record RegisterRequest(
        @NotBlank(message = "请输入用户名。") String username,
        @NotBlank(message = "请输入密码。")
        @Size(min = 8, max = 72, message = "密码需要 8–72 个字符。") String password,
        @NotBlank(message = "请再次输入密码。") String confirmPassword) {
}
