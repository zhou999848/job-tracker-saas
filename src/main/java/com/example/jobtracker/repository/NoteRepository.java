package com.example.jobtracker.repository;

import com.example.jobtracker.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByJobId(Long jobId);  // 通过 jobId 查笔记
Page<Note>findByJobId(Long jobId,Pageable pageable);
}
