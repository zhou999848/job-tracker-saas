package com.example.jobtracker.dto;

import java.time.LocalDateTime;

public class NoteDto {
    private String jobId;
    private String content;
    private LocalDateTime createdAt;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
