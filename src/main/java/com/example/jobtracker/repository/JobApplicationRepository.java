package com.example.jobtracker.repository;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.jobtracker.domain.JobApplication;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface JobApplicationRepository extends JpaRepository<JobApplication,UUID> {
    Page<JobApplication> findByUserUsername(String username, Pageable pageable);

    Page<JobApplication> findByCompanyContainingIgnoreCase(String keyword, Pageable pageable);
    List<JobApplication> findByUserId(UUID id);



}
