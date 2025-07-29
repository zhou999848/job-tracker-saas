package com.example.jobtracker.web;

import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Controller
public class PageController {

    private final JobApplicationRepository jobRepo;
    private final NoteRepository noteRepo;

    public PageController(JobApplicationRepository jobRepo, NoteRepository noteRepo) {
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
    }

    @GetMapping("/jobs")
    public String showJobs(Model model) {
        List<JobApplication> jobs = jobRepo.findAll();
        model.addAttribute("jobs", jobs);
        return "jobs";  // 指向 templates/jobs.html
    }

    @GetMapping("/notes/{jobId}")
    public String showNotes(@PathVariable UUID jobId, Model model) {
        List<Note> notes = noteRepo.findByJobId(jobId);
        model.addAttribute("notes", notes);
        return "notes";  // 指向 templates/notes.html
    }
}

