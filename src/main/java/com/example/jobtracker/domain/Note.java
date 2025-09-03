package com.example.jobtracker.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Note {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID jobId;  // 外键，不强关联

    private String content;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore                                // ✅ 避免被序列化时触发懒加载/循环
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "note_file_paths", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "path")  // 必须与数据库表名一致
    private List<String> filePaths = new ArrayList<>();
    @Transient
    private List<String> removeFiles;          // 勾选要删除的旧文件（用原路径

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

    public List<String> getRemoveFiles() { return removeFiles; }
    public void setRemoveFiles(List<String> removeFiles) { this.removeFiles = removeFiles; }

    public void setUser(User user) {this.user = user;}
    public User getUser() {return user;}


}
