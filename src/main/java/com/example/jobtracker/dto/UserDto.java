package com.example.jobtracker.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserDto {
    @NotBlank(message="username cannot be blank")
    private String username;
    @NotBlank(message="password cannot be blank")
    @Size(min = 8, max = 72, message = "Password length 8~72")
    // 至少包含大小写与数字，示例规则（可自行调整）
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Need upper, lower and digit")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
