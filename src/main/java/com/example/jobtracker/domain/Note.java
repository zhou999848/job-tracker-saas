package com.example.jobtracker.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Entity
public class Note {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID jobId;  // 外键，不强关联

    private String content;

    private LocalDateTime createdAt;

    @ManyToOne
    private User user;
    @ElementCollection
    private List<String> filePaths = new ArrayList<>();

    // Getter & Setter
    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }
    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public void setUser(User user) {this.user = user;}
    public User getUser() {return user;}

}
