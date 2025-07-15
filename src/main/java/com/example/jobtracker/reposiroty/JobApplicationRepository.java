package com.example.jobtracker.reposiroty;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.jobtracker.domain.JobApplication;
import java.util.UUID;
public interface JobApplicationRepository extends JpaRepository<JobApplication,UUID> {
}
