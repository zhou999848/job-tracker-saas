package com.example.jobtracker.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.*;

@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest req, Model model) {
        int status = Optional.ofNullable(
                        (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
                .orElse(500);

        String path = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String message = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        // ✅ 新增：取出异常对象
        Throwable ex = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        boolean isApi = path != null && path.startsWith("/api/");
        HttpStatus httpStatus = HttpStatus.resolve(status) != null
                ? HttpStatus.valueOf(status)
                : HttpStatus.INTERNAL_SERVER_ERROR;

        // ---- API 模式：返回 JSON ----
        if (isApi) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", httpStatus.value());
            body.put("error", httpStatus.getReasonPhrase());
            body.put("message",
                    (message == null || message.isBlank())
                            ? (status == 404 ? "Resource not found" : "Internal error")
                            : message);
            body.put("path", path);

            // ✅ 如果是校验错误，附带字段错误信息
            if (ex instanceof MethodArgumentNotValidException manv) {
                Map<String, String> fields = new LinkedHashMap<>();
                for (FieldError fe : manv.getBindingResult().getFieldErrors()) {
                    fields.put(fe.getField(), fe.getDefaultMessage());
                }
                body.put("fields", fields);
            }

            return ResponseEntity.status(httpStatus).body(body);
        }

        // ---- 页面模式：渲染 error.html ----
        model.addAttribute("code", httpStatus.value());

        String key = switch (httpStatus.value()) {
            case 400 -> "error.400";
            case 401 -> "error.401";
            case 403 -> "error.403";
            case 404 -> "error.404";
            default -> "error.500";
        };
Boolean showPath = switch (httpStatus.value()) {
            case 400, 401, 403, 404 -> false;
            default -> true;
        };
        model.addAttribute("msgKey", key);
        model.addAttribute("message", message);
        model.addAttribute("path", path);
        model.addAttribute("timestamp", Instant.now().toString());//new
        model.addAttribute("status", httpStatus.value());//new
        model.addAttribute("showPath",showPath);//new
        // ✅ 如果是校验错误，把所有错误文案放入 model
        if (ex instanceof MethodArgumentNotValidException manv) {
            List<String> validationMessages = new ArrayList<>();
            for (FieldError fe : manv.getBindingResult().getFieldErrors()) {
                validationMessages.add(fe.getDefaultMessage());
            }
            model.addAttribute("validationMessages", validationMessages);
        }

        return "error";
    }
}
