package com.example.jobtracker.service;

import com.example.jobtracker.domain.Note;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


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
    public Page<NoteDto> findByJobIdPaged(Long jobId, int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByJobId(jobId, request)
                .map(note -> {
                    NoteDto dto = new NoteDto();
                    dto.setJobId(note.getJobId());
                    dto.setContent(note.getContent());
                    dto.setCreatedAt(note.getCreatedAt());
                    return dto;
                });
    }
    public void saveAll(List<NoteDto> notes) {
        List<Note> toSave = notes.stream().map(dto -> {
            Note note = new Note();
            note.setJobId(dto.getJobId());
            note.setContent(dto.getContent());
            note.setCreatedAt(LocalDateTime.now());
            return note;
        }).collect(Collectors.toList());

        repository.saveAll(toSave);
    }
    public void delete(Long id) {
        repository.deleteById(id);
    }

}
