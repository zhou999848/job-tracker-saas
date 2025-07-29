package com.example.jobtracker.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class NoteDto {
    private UUID jobId;
    private String content;
    private LocalDateTime createdAt;

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
