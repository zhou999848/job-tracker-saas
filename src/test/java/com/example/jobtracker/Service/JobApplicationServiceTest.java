package com.example.jobtracker.Service;


import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.JobApplicationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Transactional
class JobApplicationServiceTest {

    @Mock JobApplicationRepository repo;
    @Mock
    CurrentTenant currentTenant;

    @InjectMocks
    JobApplicationService service;

    @BeforeEach
    void setupSecurity() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", "N/A");
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);


    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
        clearInvocations(repo, currentTenant);
    }

    @Test
    @DisplayName("searchByCompany 会按租户+用户过滤，并使用正确的分页与排序")
    void searchByCompany_filtersByTenantAndUser() {
        UUID tenantId = UUID.randomUUID();
        when(currentTenant.requireTenantId()).thenReturn(tenantId);

        JobApplication entity = new JobApplication();
        entity.setId(UUID.randomUUID());
        entity.setCompany("ABC Inc");

        // 用 any(Pageable.class) 做宽松匹配，避免 PotentialStubbingProblem
        when(repo.findByTenantIdAndCompanyContainingIgnoreCaseAndUser_Username(
                eq(tenantId), eq("abc"), eq("alice"), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0,5), 1));

        Page<JobApplicationDto> page = service.searchByCompany("abc", 0, 5);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCompany()).isEqualTo("ABC Inc");

        // 进一步：捕获实际传入的 Pageable，断言排序与分页参数
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo, times(1)).findByTenantIdAndCompanyContainingIgnoreCaseAndUser_Username(
                eq(tenantId), eq("abc"), eq("alice"), captor.capture()
        );
        Pageable actual = captor.getValue();
        assertThat(actual.getPageNumber()).isEqualTo(0);
        assertThat(actual.getPageSize()).isEqualTo(5);
        // 如果你的 Service 里确实按 appliedDate DESC 排序，这里断言：
        Sort sort = actual.getSort();
        assertThat(sort.getOrderFor("appliedDate")).isNotNull();
        assertThat(sort.getOrderFor("appliedDate").getDirection()).isEqualTo(Sort.Direction.DESC);

        verifyNoMoreInteractions(repo);
    }
}

