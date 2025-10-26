// src/test/java/.../repository/JobApplicationRepositoryIT.java
package com.example.jobtracker.repository;

import com.example.jobtracker.container.PostgresTCBase;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Tenant;

import lombok.Builder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
@Builder
@DataJpaTest
@ActiveProfiles("test") // 可选：使用专用的 test 配置
    @Transactional
class JobApplicationRepositoryIT extends PostgresTCBase {

    @Autowired
    JobApplicationRepository repo;
    private UUID tenant1;
    private UUID tenant2;


    @BeforeEach
    void setUp() {
        var t1 = UUID.randomUUID();
        var t2 = UUID.randomUUID();

        tenant1 = t1;
        tenant2 = t2;

        repo.deleteAll();

        repo.save(JobApplication.builder()
                .tenant(Tenant.builder().id(t1).build()).company("ABC Inc").position("Backend")
                .createdAt(Instant.now()).build());

        repo.save(JobApplication.builder()
                .tenant(Tenant.builder().id(t2).build()).company("ABC Inc").position("Frontend")
                .createdAt(Instant.now()).build());

        repo.save(JobApplication.builder()
                .tenant(Tenant.builder().id(t1).build()).company("XYZ LLC").position("SWE")
                .createdAt(Instant.now()).build());
    }

    @Test
    @DisplayName("findByTenantAndCompany：只返回该租户的数据")
    void findByTenantAndCompany_onlyOwnTenant() {
      //  var t1 = repo.findAll().stream().findFirst().map(JobApplication::getTenantId).orElseThrow();
        var t1 = tenant1;

        var page = repo.findByTenantIdAndCompanyContainingIgnoreCaseAndUser_Username(
                t1,"ABC", "system", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCompany()).isEqualTo("ABC Inc");
    }
}
