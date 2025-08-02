package com.example.jobtracker.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**

 ✅ 全局异常处理类 / Global Exception Handler
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    /**

     ✅ 处理参数校验失败 / Handle validation failure

     [Exception] MethodArgumentNotValidException
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String username = getCurrentUsername();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        logger.warn("[Validation Error] User={} - 参数校验失败 / Validation failed: {}", username, errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**

     ✅ 通用异常处理 / Handle general exceptions (e.g. file not found, type mismatch)

     [Exception] Exception
     */
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleOther(Exception ex) {
        String username = getCurrentUsername();
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());

        logger.error("[Unhandled Exception] User={} - 未处理异常 / Exception occurred: {}", username, ex.getMessage());
        return error;
    }
}

