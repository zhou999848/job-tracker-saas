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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // ← 必须加！不然会用 H2
@ActiveProfiles("test")
@Import({TestDatabaseConfig.class, TestS3Config.class})  // 如果你有自定义配置也加上
class JobApplicationRepositoryIT extends PostgresTCBase {

    @Autowired private JobApplicationRepository repo;
    @Autowired private TenantRepository tenantRepo;

    private UUID tenant1;
    private UUID tenant2;

    @BeforeEach
    void setUp() {
        repo.deleteAllInBatch();
        tenantRepo.deleteAllInBatch();


        // ← 关键：不要手动设置 id！让 Hibernate 自己生成
        Tenant t1 = tenantRepo.save(Tenant.builder()
                .name("Tenant One")
                .build());
        Tenant t2 = tenantRepo.save(Tenant.builder()
                .name("Tenant Two")
                .build());

        tenant1 = t1.getId();
        tenant2 = t2.getId();

        // JobApplication 也一样不要手动 set id
        repo.saveAll(List.of(
                JobApplication.builder()
                        .tenant(t1)
                        .company("ABC Inc")
                        .position("Backend")
                        .createdAt(Instant.now())
                        .build(),
                JobApplication.builder()
                        .tenant(t2)
                        .company("ABC Inc")
                        .position("Frontend")
                        .createdAt(Instant.now())
                        .build(),
                JobApplication.builder()
                        .tenant(t1)
                        .company("XYZ LLC")
                        .position("SWE")
                        .createdAt(Instant.now())
                        .build()
        ));
    }

    // 测试方法完全不动
}