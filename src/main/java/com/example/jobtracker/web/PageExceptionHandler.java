package com.example.jobtracker.web;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@ControllerAdvice(annotations = Controller.class) // 仅作用于 @Controller（页面）
@Order(2)
public class PageExceptionHandler {

    // 表单校验错误 → 返回 JSON 也可以，但更建议返回错误页/重定向；这里先返回最简单 JSON 以便你观察
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Map<String, Object> handleValidation(Exception ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("error", "VALIDATION_FAILED");
        errors.put("message", ex.getMessage());
        return errors;
    }

    // ❌ 不要写 @ExceptionHandler(Exception.class) 大兜底，避免与 API 处理器互相抢夺/递归
}
