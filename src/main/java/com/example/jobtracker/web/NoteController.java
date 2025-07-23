package com.example.jobtracker.web;

import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.service.NoteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;


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
    @GetMapping("/{jobId}/paged")
    public Page<NoteDto> getPagedNotes(@PathVariable Long jobId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size) {
        return service.findByJobIdPaged(jobId, page, size);
    }
    @PostMapping("/batch")
    public void batchCreate(@RequestBody List<NoteDto> notes) {
        service.saveAll(notes);
    }
    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable Long id) {
        service.delete(id);
    }

}
