package com.example.jobtracker.Service;



import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.dto.TenantDto;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.service.TenantService;
import com.example.jobtracker.ああ７a５.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createTenant_success() {
        String name = "NewTenant";

        when(tenantRepo.findByName(name)).thenReturn(Optional.empty());
        when(tenantRepo.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        UUID newId = tenantService.createTenant(name);

        assertNotNull(newId);
        verify(tenantRepo).save(any(Tenant.class));
    }

    @Test
    void createTenant_duplicateName_throwsException() {
        String name = "ExistingTenant";
        Tenant existing = new Tenant();
        existing.setName(name);

        when(tenantRepo.findByName(name)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> tenantService.createTenant(name));
    }

    @Test
    void list_callsRepository_findTenantAdminPage() {
        String q = "search";
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Map<String,Object>> mockPage = new PageImpl<>(List.of(Map.of("id", UUID.randomUUID())));
        when(tenantRepo.findTenantAdminPage(q, pageable)).thenReturn(mockPage);

        Page<Map<String,Object>> result = tenantService.list(q, pageable);

        assertEquals(1, result.getTotalElements());
        verify(tenantRepo).findTenantAdminPage(q, pageable);
    }

    @Test
    void getTenant_success() {
        UUID tenantId = UUID.randomUUID();
        Tenant t = new Tenant();
        t.setId(tenantId);

        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(t));

        Tenant result = tenantService.getTenant(tenantId);
        assertEquals(tenantId, result.getId());
    }

    @Test
    void getTenant_notFound_throwsException() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepo.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> tenantService.getTenant(tenantId));
    }
}
