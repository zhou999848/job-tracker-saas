// src/test/java/com/example/jobtracker/repository/JobApplicationRepositoryIT.java
package com.example.jobtracker.repository;

import com.example.jobtracker.config.TestDatabaseConfig;
import com.example.jobtracker.config.TestS3Config;
import com.example.jobtracker.container.PostgresTCBase;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Tenant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JobApplicationRepositoryIT extends PostgresTCBase {

    @Autowired
    private JobApplicationRepository repo;

    @Autowired
    private TenantRepository tenantRepo;

    private UUID tenant1;
    private UUID tenant2;

    @BeforeEach
    void setUp() {
        tenant1 = UUID.randomUUID();
        tenant2 = UUID.randomUUID();

        repo.deleteAllInBatch();
        tenantRepo.deleteAllInBatch();

        Tenant t1 = tenantRepo.save(Tenant.builder().id(tenant1).name("Tenant One").build());
        Tenant t2 = tenantRepo.save(Tenant.builder().id(tenant2).name("Tenant Two").build());

        repo.saveAll(List.of(
                JobApplication.builder()
                        .id(UUID.randomUUID())
                        .tenant(t1)
                        .company("ABC Inc")
                        .position("Backend")
                        .createdAt(Instant.now())
                        .build(),
                JobApplication.builder()
                        .id(UUID.randomUUID())
                        .tenant(t2)
                        .company("ABC Inc")
                        .position("Frontend")
                        .createdAt(Instant.now())
                        .build(),
                JobApplication.builder()
                        .id(UUID.randomUUID())
                        .tenant(t1)
                        .company("XYZ LLC")
                        .position("SWE")
                        .createdAt(Instant.now())
                        .build()
        ));
    }

    @Test
    @DisplayName("findByTenantAndCompany丗扅曉夞?慸?揑悢悩")
    void findByTenantAndCompany_onlyOwnTenant() {
        var page = repo.findByTenantIdAndCompanyContainingIgnoreCaseAndUser_Username(
                tenant1, "ABC", "system", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCompany()).isEqualTo("ABC Inc");
    }
}
