package com.example.jobtracker.domain;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
import java.time.LocalDate;//this is for date handling
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor



@Entity
@Table(name = "job-application")           // ✅ 下划线命名
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)   // ← 加上这行！！！
    private UUID id = UUID.randomUUID();


    private String company;
    private String position;
    private String status;
    private LocalDate appliedDate;

    @Column(name = "file_path")              // ✅ 下划线命名
    private String filePath;
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;    // ✅ 新增：所属租户


    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore                               // ✅ API 出口避免序列化 user（防递归/懒加载）
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt=Instant.now();

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }


    public Tenant getTenant() {return tenant;}
   public void setTenant(Tenant tenant) {this.tenant=tenant;}
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
