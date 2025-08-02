package com.example.jobtracker.service;

import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public void save(NoteDto dto) {
        // ① 获取当前登录的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // ② 查找数据库中对应的 User 对象
        User user = userRepository.findByUsername(username).orElseThrow();

        // ③ 创建 Note 实体对象，并设置字段
        Note note = new Note();

        note.setContent(dto.getContent());       // 设置内容
        note.setCreatedAt(dto.getCreatedAt());   // 设置时间（如果有）
        note.setJobId(dto.getJobId());           // 设置关联职位ID（外键）

        // ④ 绑定当前用户
        note.setUser(user);

        // ⑤ 保存
        noteRepository.save(note);
    }


    public List<NoteDto> findByJobId(UUID jobId) {
        // 获取当前登录用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 只查当前用户的笔记
        return noteRepository.findByUserUsernameAndJobId(username, jobId).stream().map(note -> {
            NoteDto dto = new NoteDto();

            dto.setJobId(note.getJobId());
            dto.setCreatedAt(note.getCreatedAt());
            return dto;
        }).toList();
    }

    public Page<NoteDto> findByJobIdPaged(UUID jobId, int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noteRepository.findByJobId(jobId, request)
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

        noteRepository.saveAll(toSave);
    }

    public void delete(UUID id) {
        noteRepository.deleteById(id);
    }

    public void save(Note note) {
        noteRepository.save(note);
    }

    public void checkOwner(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();//获取当前登录用户

        Note note = noteRepository.findById(id)//加载note，并检查是不是当前用户的
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!note.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        noteRepository.delete(note);
    }

    public Note findById(UUID id) {//4-5対応controller。findById
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该职位"));
    }
}

