package com.example.jobtracker.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

    // 这个类会处理所有接口的校验错误
    @RestControllerAdvice
    public class GlobalExceptionHandler {

        // 处理参数验证失败的异常
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
            Map<String, String> errors = new HashMap<>();

            // 从异常中取出所有字段错误
            ex.getBindingResult().getFieldErrors().forEach(error -> {
                // 哪个字段错了
                // 对应的错误信息
                errors.put(error.getField(), error.getDefaultMessage());
            });

            // 返回 400 状态码 + 错误信息
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        //文件类型错误 / 文件未找到等通用异常
        @ExceptionHandler(Exception.class)
        public Map<String, String> handleOther(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return error;
    }
}