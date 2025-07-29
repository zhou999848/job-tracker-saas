package com.example.jobtracker.domain;

import jakarta.persistence.*;
import java.util.UUID;



import java.time.LocalDate;
@Entity
@Table(name="job-application")
public class JobApplication {
    @Id
    private UUID id = UUID.randomUUID();

    private String company;
    private String position;
    private String status;
    private LocalDate appliedDate;
    @Column(name="file-path")
    private String filePath;
    public void setFilePath(String filePath) {this.filePath=filePath;}
    public String getFilePath() {return this.filePath;}
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
