package com.example.huarongdao.web;

import com.example.huarongdao.auth.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/** 将业务异常、参数校验错误转换成可读的 JSON。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> api(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage())
                .orElse("参数不合法");
        return ResponseEntity.badRequest().body(ApiError.of(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> badJson() {
        return ResponseEntity.badRequest().body(ApiError.of("请求 JSON 格式不正确。"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        LOG.error("未处理的服务器异常", exception);
        return ResponseEntity.internalServerError().body(ApiError.of("服务器暂时无法处理请求。"));
    }
}
