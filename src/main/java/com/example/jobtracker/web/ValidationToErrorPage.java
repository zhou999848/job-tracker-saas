package com.example.jobtracker.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice   // 只针对页面 Controller 生效（非 @RestController）
@Component
public class ValidationToErrorPage {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationError(HttpServletRequest request,
                                        MethodArgumentNotValidException ex) {
        // 设置错误信息，让 AppErrorController 能取到
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 400);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ex);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "Validation failed");

        // forward 到 /error，由 AppErrorController 统一处理
        return "forward:/error";
    }
}
