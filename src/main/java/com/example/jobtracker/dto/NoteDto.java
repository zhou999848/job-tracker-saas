package com.example.jobtracker.dto;

import java.time.LocalDateTime;

public class NoteDto {
    private Long jobId;
    private String content;
    private LocalDateTime createdAt;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
