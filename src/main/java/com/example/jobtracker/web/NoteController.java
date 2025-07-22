package com.example.jobtracker.web;

import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PostMapping
    public void create(@RequestBody NoteDto dto) {
        service.save(dto);
    }

    @GetMapping("/{jobId}")
    public List<NoteDto> getNotes(@PathVariable Long jobId) {
        return service.findByJobId(jobId);
    }
}
