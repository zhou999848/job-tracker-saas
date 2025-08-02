package com.example.jobtracker.web;

import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    private final JobApplicationRepository jobRepo;
    private final NoteRepository noteRepo;

    public PageController(JobApplicationRepository jobRepo, NoteRepository noteRepo) {
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * ✅ 显示所有职位页面 / Display all job applications page
     * [GET] /jobs
     */
    @GetMapping("/jobs")
    public String showJobs(Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Jobs] User={} 查看职位列表 / Viewing job list", username);

        List<JobApplication> jobs = jobRepo.findAll();
        model.addAttribute("jobs", jobs);
        return "jobs";  // 指向 templates/jobs.html
    }

    /**
     * ✅ 显示某个职位的笔记页面 / Display notes for a job application
     * [GET] /notes/{jobId}
     */
    @GetMapping("/notes/{jobId}")
    public String showNotes(@PathVariable UUID jobId, Model model, Pageable request) {
        String username = getCurrentUsername();
        logger.info("[Show Notes] User={} 查看 jobId={} 的笔记 / Viewing notes for jobId={}", username, jobId, jobId);

        List<Note> notes = (List<Note>) noteRepo.findByJobId(jobId, request);
        model.addAttribute("notes", notes);
        return "notes";  // 指向 templates/notes.html
    }
}