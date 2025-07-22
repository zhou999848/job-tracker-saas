package com.example.jobtracker.repository;

import com.example.jobtracker.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByJobId(Long jobId);  // 通过 jobId 查笔记
}
