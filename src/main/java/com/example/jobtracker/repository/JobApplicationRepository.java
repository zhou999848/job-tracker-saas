package com.example.jobtracker.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.jobtracker.domain.JobApplication;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface JobApplicationRepository extends JpaRepository<JobApplication,UUID> {
    Page<JobApplication> findAll(Pageable pageable);
    Page<JobApplication> findByCompanyContainingIgnoreCase(String keyword, Pageable pageable);

}
