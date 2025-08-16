package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;

import jakarta.transaction.Transactional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;


@Service
public class NoteService {
    private final NoteRepository noteRepo;
    private final UserRepository userRepo;
    private final JobApplicationRepository jobRepo;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository, JobApplicationRepository jobRepo) {
        this.noteRepo = noteRepository;
        this.userRepo= userRepository;
        this.jobRepo = jobRepo;
    }

    public void save(NoteDto dto) {
        // ① 获取当前登录的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // ② 查找数据库中对应的 User 对象
        User user = userRepo.findByUsername(username).orElseThrow();

        Note note = new Note();

        // ③ 创建 Note 实体对象，并设置字段

        note.setContent(dto.getContent());       // 设置内容
        note.setCreatedAt(dto.getCreatedAt());   // 设置时间（如果有）
        note.setJobId(dto.getJobId());           // 设置关联职位ID（外键）
        note.setFilePaths(dto.getFilePaths());
        // ④ 绑定当前用户
        note.setUser(user);

        // ⑤ 保存
        noteRepo.save(note);
    }

    public void save(Note note) {
        noteRepo.save(note);
    }


    public Page<NoteDto> findByJobIdPaged(UUID jobId, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noteRepo.findByUserUsernameAndJobId(username, jobId, request)
                .map(note -> {
                    NoteDto dto = new NoteDto();
                    dto.setJobId(note.getJobId());
                    dto.setContent(note.getContent());
                    dto.setCreatedAt(note.getCreatedAt());
                    dto.setFilePaths(note.getFilePaths() == null ? List.of() : new ArrayList<>(note.getFilePaths()));// 设置附件路径
                    return dto;
                });
    }


    public void delete(UUID id) {
        noteRepo.deleteById(id);
    }


    @Transactional
    public void checkOwner(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();//获取当前登录用户

        Note note = noteRepo.findById(id)//加载note，并检查是不是当前用户的
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!note.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        noteRepo.delete(note);
    }

    public Note findById(UUID id) {//4-5対応controller。findById
        return noteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该职位"));
    }





}
