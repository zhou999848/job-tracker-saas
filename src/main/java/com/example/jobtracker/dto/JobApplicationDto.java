package com.example.jobtracker.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO: 用于与前端通信的数据结构
 */
public class JobApplicationDto {
    private UUID id;
    private String company;
    private String position;
    private String status;
    private LocalDate appliedDate;

    // Getter & Setter 省略可用 Lombok（如 @Getter/@Setter）
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }
}
