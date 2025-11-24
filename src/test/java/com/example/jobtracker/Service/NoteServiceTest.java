package com.example.jobtracker.Service;


import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.NoteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private JobApplicationRepository jobRepo;

    @Mock
    private CurrentTenant currentTenant;

    @InjectMocks
    private NoteService noteService;

    private UUID tenantId;
    private String username;

    @BeforeEach
    void setUpSecurityContext() {
        tenantId = UUID.randomUUID();
        username = "alice";

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void save_withDto_success() {
        UUID jobId = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();
        NoteDto dto = new NoteDto();
        dto.setJobId(jobId);
        dto.setContent("note content");
        dto.setCreatedAt(now);

        dto.setFilePaths(List.of("file1.txt", "file2.txt"));

        User user = new User();
        user.setUsername(username);

        JobApplication job = new JobApplication();
        job.setId(jobId);
        // 假设 JobApplication 有 getTenant()/setTenant()
        com.example.jobtracker.domain.Tenant tenant = new com.example.jobtracker.domain.Tenant();
        tenant.setId(tenantId);
        job.setTenant(tenant);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(userRepo.findByTenantIdAndUsername(tenantId, username))
                .thenReturn(Optional.of(user));
        when(jobRepo.findByIdAndTenantId(jobId, tenantId))
                .thenReturn(Optional.of(job));

        noteService.save(dto);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepo).save(noteCaptor.capture());

        Note saved = noteCaptor.getValue();
        assertEquals(jobId, saved.getJobId());
        assertEquals("note content", saved.getContent());
        assertEquals(now, saved.getCreatedAt());
        assertEquals(user, saved.getUser());
        assertEquals(tenant, saved.getTenant());
        assertEquals(List.of("file1.txt", "file2.txt"), saved.getFilePaths());
    }

    @Test
    void save_withDto_jobNotFound_throwsNotFound() {
        UUID jobId = UUID.randomUUID();
        NoteDto dto = new NoteDto();
        dto.setJobId(jobId);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(userRepo.findByTenantIdAndUsername(tenantId, username))
                .thenReturn(Optional.of(new User()));
        when(jobRepo.findByIdAndTenantId(jobId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> noteService.save(dto)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void save_withNote_fillMissingFields_success() {
        UUID jobId = UUID.randomUUID();

        Note note = new Note();
        note.setJobId(jobId);
        note.setContent("content");

        User user = new User();
        user.setUsername(username);

        com.example.jobtracker.domain.Tenant tenant = new com.example.jobtracker.domain.Tenant();
        tenant.setId(tenantId);

        JobApplication job = new JobApplication();
        job.setId(jobId);
        job.setTenant(tenant);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(userRepo.findByTenantIdAndUsername(tenantId, username))
                .thenReturn(Optional.of(user));
        when(jobRepo.findByIdAndTenantId(jobId, tenantId))
                .thenReturn(Optional.of(job));

        noteService.save(note);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepo).save(noteCaptor.capture());

        Note saved = noteCaptor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(jobId, saved.getJobId());
        assertEquals(tenant, saved.getTenant());
    }

    @Test
    void save_withNote_missingJobId_throwsBadRequest() {
        Note note = new Note();
        // jobId 为 null

        when(currentTenant.requireTenantId()).thenReturn(tenantId);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> noteService.save(note)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void save_withNote_crossTenantJob_throwsForbidden() {
        UUID jobId = UUID.randomUUID();
        Note note = new Note();
        note.setJobId(jobId);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(userRepo.findByTenantIdAndUsername(tenantId, username))
                .thenReturn(Optional.of(new User()));
        when(jobRepo.findByIdAndTenantId(jobId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> noteService.save(note)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void findByJobIdPaged_success() {
        UUID jobId = UUID.randomUUID();

        Note n = new Note();
        n.setId(UUID.randomUUID());
        n.setJobId(jobId);
        n.setContent("c1");
        n.setCreatedAt(LocalDateTime.now());
        n.setFilePaths(List.of("a", "b"));

        PageRequest request = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Note> page = new PageImpl<>(List.of(n), request, 1);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(noteRepo.findByTenantIdAndUser_UsernameAndJobId(tenantId, username, jobId, request))
                .thenReturn(page);

        Page<NoteDto> result = noteService.findByJobIdPaged(jobId, 0, 10);

        assertEquals(1, result.getTotalElements());
        NoteDto dto = result.getContent().get(0);
        assertEquals(n.getId(), dto.getId());
        assertEquals(jobId, dto.getJobId());
        assertEquals("c1", dto.getContent());
        assertEquals(n.getCreatedAt(), dto.getCreatedAt());
        assertEquals(List.of("a", "b"), dto.getFilePaths());
    }

    @Test
    void delete_success() {
        UUID noteId = UUID.randomUUID();

        User user = new User();
        user.setUsername(username);

        Note note = new Note();
        note.setId(noteId);
        note.setUser(user);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(noteRepo.findByIdAndTenantId(noteId, tenantId))
                .thenReturn(Optional.of(note));

        noteService.delete(noteId);

        verify(noteRepo).delete(note);
    }

    @Test
    void delete_notOwner_throwsForbidden() {
        UUID noteId = UUID.randomUUID();

        User user = new User();
        user.setUsername("other");  // 不同 user

        Note note = new Note();
        note.setId(noteId);
        note.setUser(user);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(noteRepo.findByIdAndTenantId(noteId, tenantId))
                .thenReturn(Optional.of(note));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> noteService.delete(noteId)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(noteRepo, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFound() {
        UUID noteId = UUID.randomUUID();

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(noteRepo.findByIdAndTenantId(noteId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> noteService.delete(noteId)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void findById_success() {
        UUID noteId = UUID.randomUUID();
        Note note = new Note();
        note.setId(noteId);

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(noteRepo.findByIdAndTenantId(noteId, tenantId))
                .thenReturn(Optional.of(note));

        Note result = noteService.findById(noteId);
        assertEquals(noteId, result.getId());
    }

    @Test
    void findById_notFound_throwsNotFound() {
        UUID noteId = UUID.randomUUID();

        when(currentTenant.requireTenantId()).thenReturn(tenantId);
        when(noteRepo.findByIdAndTenantId(noteId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> noteService.findById(noteId)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
