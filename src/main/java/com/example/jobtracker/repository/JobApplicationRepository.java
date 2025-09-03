package com.example.jobtracker.repository;
import com.example.jobtracker.dto.JobApplicationDto;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.AbstractPersistable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.jobtracker.domain.JobApplication;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface JobApplicationRepository extends JpaRepository<JobApplication,UUID> {
    Page<JobApplication> findByUserUsername(String username, Pageable pageable);

    // Repo 例子（方法名可以不同，但要包含 username 过滤）
    Page<JobApplication> findByCompanyContainingIgnoreCaseAndUser_Username(
            String keyword, String username, Pageable pageable);


}
