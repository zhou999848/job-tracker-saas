package com.example.jobtracker.dto;

public record ApiResponse(boolean ok, String message, Object data, String error) {
    public static ApiResponse ok(String message) { return new ApiResponse(true, message, null, null); }
    public static ApiResponse ok(String message, Object data) { return new ApiResponse(true, message, data, null); }
    public static ApiResponse error(String error) { return new ApiResponse(false, null, null, error); }
}

