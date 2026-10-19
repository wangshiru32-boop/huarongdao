package com.example.huarongdao.web;

/** 所有 API 错误使用统一 JSON 结构：{ok:false,message:"..."}。 */
public record ApiError(boolean ok, String message) {
    public static ApiError of(String message) {
        return new ApiError(false, message);
    }
}
