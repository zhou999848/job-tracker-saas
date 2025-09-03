package com.example.jobtracker.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;



import java.time.LocalDate;//this is for date handling
@Entity
@Table(name = "job-application")           // ✅ 下划线命名
public class JobApplication {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore                               // ✅ API 出口避免序列化 user（防递归/懒加载）
    private User user;

    private String company;
    private String position;
    private String status;
    private LocalDate appliedDate;

    @Column(name = "file_path")              // ✅ 下划线命名
    private String filePath;

    public void setFilePath(String filePath) {this.filePath=filePath;}
    public String getFilePath() {return this.filePath;}
    public UUID getId() {return id;}
    public void setId(UUID id) {this.id = id;}

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

    public void setUser(User user){this.user=user;}
    public User getUser(){return this.user;}

}
