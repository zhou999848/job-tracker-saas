package com.example.jobtracker.web;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.security.core.context.SecurityContextHolder;

@ControllerAdvice
public class GlobalModelAttributes {

    /** 给所有 Thymeleaf 页面提供 currentUser 变量 */
    @ModelAttribute("currentUser")
    public String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        var name = auth.getName();
        return ("anonymousUser".equals(name)) ? null : name;
    }
}
