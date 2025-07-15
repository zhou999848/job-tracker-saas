package com.example.jobtracker.domain;

import jakarta.persistence.*;



import java.util.UUID;
import java.time.LocalDate;
@Entity
@Table(name="job-application")
public class JobApplication {
    @Id
    private UUID id=UUID.randomUUID();
    private String company;
    private String position;
    private String status;
    private LocalDate appliedDate;

}
