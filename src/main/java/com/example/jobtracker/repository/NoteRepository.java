package com.example.jobtracker.repository;

import com.example.jobtracker.domain.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findByJobId(UUID jobId);  // 通过 jobId 查笔记
Page<Note>findByJobId(UUID jobId, Pageable pageable);






}
