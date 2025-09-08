package com.example.jobtracker.web;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice // 或者放在你的 Controller 上
public class GlobalStringTrimBinder {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new org.springframework.beans.propertyeditors.StringTrimmerEditor(true) {
            @Override
            public void setAsText(String text) {
                if (text == null) {
                    super.setAsText(null);
                    return;
                }
                // 1) 全角空格 -> 半角空格
                String normalized = text.replace('\u3000', ' ');
                // 2) 折叠所有空白（包括制表/换行等）为单个半角空格
                normalized = normalized.replaceAll("\\s+", " ");
                // 3) trim，两端清空后若为空，设为 null（使 @NotBlank/@NotNull 生效）
                normalized = normalized.trim();
                super.setAsText(normalized.isEmpty() ? null : normalized);
            }
        });
    }
}



