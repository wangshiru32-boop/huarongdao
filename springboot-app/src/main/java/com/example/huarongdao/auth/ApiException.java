package com.example.huarongdao.auth;

/** 可安全返回给浏览器的业务异常。 */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
