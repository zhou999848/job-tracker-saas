package com.example.jobtracker.repository;

import com.example.jobtracker.domain.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    @EntityGraph(attributePaths = "filePaths")
    Page<Note> findByUserUsernameAndJobId(String username, UUID jobId, Pageable pageable); // ★ 加上 Pageable

    Optional<Note> findByFilePathsContains(String filePath);
}







