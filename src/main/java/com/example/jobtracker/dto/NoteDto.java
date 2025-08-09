package com.example.jobtracker.dto;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteDto {
    private UUID id;
    private UUID jobId;
    private String content;
    private LocalDateTime createdAt;
    @ElementCollection
    @CollectionTable(name = "note_file_paths", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "path")  // 必须与数据库表名一致
    private List<String> filePaths= new ArrayList<>(); // 附件路径

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
}