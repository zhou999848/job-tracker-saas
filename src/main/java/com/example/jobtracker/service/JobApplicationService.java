package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.reposiroty.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobApplicationService {
    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public void save(JobApplicationDto dto) {
        JobApplication job = new JobApplication();
        job.setCompany(dto.getCompany());
        job.setPosition(dto.getPosition());
        job.setStatus(dto.getStatus());
        job.setAppliedDate(dto.getAppliedDate());

        repository.save(job);
    }

    public List<JobApplicationDto> findAll() {
        List<JobApplicationDto> dtoList = new ArrayList<>();
        for (JobApplication job : repository.findAll()) {
            JobApplicationDto dto = new JobApplicationDto();
            dto.setCompany(job.getCompany());
            dto.setPosition(job.getPosition());
            dto.setStatus(job.getStatus());
            dto.setAppliedDate(job.getAppliedDate());
            dtoList.add(dto);
        }
        return dtoList;
    }
}
