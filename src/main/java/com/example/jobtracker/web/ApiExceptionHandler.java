package com.example.jobtracker.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class) // 仅作用于 @RestController
@Order(1) // 先于页面级处理器
public class ApiExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleAny(Throwable ex, HttpServletRequest req) {
        // 只处理 API；否则**不处理**，交给页面链路，避免互相递归
        final String uri = req.getRequestURI();
        if (!uri.startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "SERVER_ERROR", "message", "Please check server logs."));
        }

        HttpStatus status = mapToStatus(ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("path", uri);
        body.put("message", ex.getMessage());

        // **注意**：这里绝不再抛异常，避免递归
        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus mapToStatus(Throwable ex) {
        if (ex instanceof org.springframework.security.access.AccessDeniedException) return HttpStatus.FORBIDDEN;
        if (ex instanceof ResponseStatusException rse) return HttpStatus.valueOf(rse.getStatusCode().value());
        if (ex instanceof java.util.NoSuchElementException) return HttpStatus.NOT_FOUND;
        if (ex instanceof IllegalArgumentException) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
