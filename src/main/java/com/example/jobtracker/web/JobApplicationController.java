package com.example.jobtracker.web;

import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.JobApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobApplicationController {
    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public void create(@RequestBody JobApplicationDto dto) {
        service.save(dto);
    }

    @GetMapping
    public List<JobApplicationDto> list() {
        return service.findAll();
    }
}
