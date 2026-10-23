package com.codeclinic.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException e) {
        int code = e.getCode();
        if (code < 400 || code > 599) code = 500;
        return ResponseEntity.status(HttpStatusCode.valueOf(code)).body(ApiResult.error(code, e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<Void>> handleValidation(Exception e) {
        var errors = e instanceof MethodArgumentNotValidException m ? m.getBindingResult() : ((BindException) e).getBindingResult();
        String message = errors.getFieldErrors().isEmpty() ? "请求参数不正确" : errors.getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResult.error(400, message));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResult<Void>> handleBadRequest(Exception e) {
        return ResponseEntity.badRequest().body(ApiResult.error(400, "请求参数不正确"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
        log.error("系统处理失败", e);
        return ResponseEntity.internalServerError().body(ApiResult.error(500, "系统处理失败"));
    }
}
