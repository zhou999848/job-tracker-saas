package com.example.jobtracker.service;

import com.example.jobtracker.domain.Note;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {
    private final NoteRepository repository;

    public NoteService(NoteRepository repository) {
        this.repository = repository;
    }

    public void save(NoteDto dto) {
        Note note = new Note();
        note.setJobId(dto.getJobId());
        note.setContent(dto.getContent());
        note.setCreatedAt(LocalDateTime.now());

        repository.save(note);
    }

    public List<NoteDto> findByJobId(Long jobId) {
        return repository.findByJobId(jobId).stream().map(note -> {
            NoteDto dto = new NoteDto();
            dto.setJobId(note.getJobId());
            dto.setContent(note.getContent());
            dto.setCreatedAt(note.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}
