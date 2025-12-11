//package com.example.jobtracker.Service;
//
//import com.example.jobtracker.TenantInvite7a4.Role;
//import com.example.jobtracker.domain.Tenant;
//import com.example.jobtracker.domain.User;
//import com.example.jobtracker.dto.ChangePasswordRequest;
//import com.example.jobtracker.dto.UpdateProfileRequest;
//import com.example.jobtracker.dto.UserDto;
//import com.example.jobtracker.exception.DuplicateUsernameException;
//import com.example.jobtracker.repository.TenantRepository;
//import com.example.jobtracker.repository.UserRepository;
//import com.example.jobtracker.service.CurrentTenant;
//import com.example.jobtracker.service.UserService;
//import io.micrometer.common.lang.Nullable;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.time.Instant;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepo;
//
//    @Mock
//    private TenantRepository tenantRepo;
//
//    @Mock
//    private PasswordEncoder encoder;
//
//    @Mock
//    private CurrentTenant currentTenant;
//
//    @InjectMocks
//    private UserService userService;
//
//    private UUID tenantId;
//    private Tenant tenant;
//
//    @BeforeEach
//    void setUp() {
//        tenantId = UUID.randomUUID();
//        tenant = new Tenant();
//        // 假设 Tenant 有 setId 方法（如果是 @Builder/@AllArgsConstructor，自行调整）
//        tenant.setId(tenantId);
//        tenant.setName("TestTenant");
//    }
//
//    @Test
//    void registerViaAdminOrInvite_success_withPathTenantId() {
//        UserDto dto = new UserDto();
//        dto.setUsername("  alice ");   // 带空格测试 trim()
//        dto.setPassword("raw-pass");
//        dto.setRole("USER");
//
//        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
//        when(userRepo.existsByTenant_IdAndUsername(tenantId, "alice"))
//                .thenReturn(false);
//        when(encoder.encode("raw-pass")).thenReturn("encoded-pass");
//
//        // save 时给 user 设置 id，方便断言
//        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
//            User u = invocation.getArgument(0);
//            u.setId(UUID.randomUUID());
//            return u;
//        });
//
//        String newUserId = userService.registerViaAdminOrInvite(dto, tenantId);
//
//        assertNotNull(newUserId);
//
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//        verify(userRepo).save(userCaptor.capture());
//
//        User saved = userCaptor.getValue();
//        assertEquals("alice", saved.getUsername());
//        assertEquals("encoded-pass", saved.getPassword());
//        assertEquals(tenant, saved.getTenant());
//        assertEquals(Role.USER, saved.getRole());
//    }
//
//    @Test
//    void registerViaAdminOrInvite_duplicateUsername_throwsException() {
//        UserDto dto = new UserDto();
//        dto.setUsername("bob");
//        dto.setPassword("xxx");
//        dto.setTenantId(tenantId);
//
//        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
//        when(userRepo.existsByTenant_IdAndUsername(tenantId, "bob"))
//                .thenReturn(true);
//
//        assertThrows(DuplicateUsernameException.class,
//                () -> userService.registerViaAdminOrInvite(dto, null));
//    }
//
//    @Test
//    void registerViaAdminOrInvite_missingTenantId_throwsException() {
//        UserDto dto = new UserDto();
//        dto.setUsername("bob");
//        dto.setPassword("xxx");
//        // pathTenantId = null, dto.tenantId = null
//
//        IllegalArgumentException ex = assertThrows(
//                IllegalArgumentException.class,
//                () -> userService.registerViaAdminOrInvite(dto, null)
//        );
//        assertTrue(ex.getMessage().contains("tenantId is required"));
//    }
//
//    @Test
//    void register_delegatesToRegisterViaAdminOrInvite() {
//        UserDto dto = new UserDto();
//        dto.setUsername("bob");
//        dto.setPassword("xxx");
//        dto.setTenantId(tenantId);
//
//        when(tenantRepo.findById(tenantId)).thenReturn(Optional.of(tenant));
//        when(userRepo.existsByTenant_IdAndUsername(tenantId, "bob"))
//                .thenReturn(false);
//        when(encoder.encode("xxx")).thenReturn("encoded");
//        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
//            User u = invocation.getArgument(0);
//            u.setId(UUID.randomUUID());
//            return u;
//        });
//
//        assertDoesNotThrow(() -> userService.registerViaAdminOrInvite(UserDto, pathTenantId);
//        verify(userRepo).save(any(User.class));
//    }
//
//    @Test
//    void updateProfile_success() {
//        UUID tid = UUID.randomUUID();
//        User user = new User();
//        user.setUsername("alice");
//
//        when(currentTenant.requireTenantId()).thenReturn(tid);
//        when(userRepo.findByTenantIdAndUsername(tid, "alice"))
//                .thenReturn(Optional.of(user));
//
//        UpdateProfileRequest req = new UpdateProfileRequest();
//        req.setDisplayName("Alice Zhang");
//        req.setEmail("alice@example.com");
//
//        userService.updateProfile("alice", req);
//
//        assertEquals("Alice Zhang", user.getDisplayName());
//        assertEquals("alice@example.com", user.getEmail());
//    }
//
//    @Test
//    void updateProfile_userNotFound_throwsRuntimeException() {
//        when(currentTenant.requireTenantId()).thenReturn(tenantId);
//        when(userRepo.findByTenantIdAndUsername(tenantId, "unknown"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(RuntimeException.class,
//                () -> userService.updateProfile("unknown", new UpdateProfileRequest()));
//    }
//
//    @Test
//    void changePassword_success() {
//        UUID tid = UUID.randomUUID();
//        User user = new User();
//        user.setUsername("alice");
//        user.setPassword("old-encoded");
//
//        ChangePasswordRequest req = new ChangePasswordRequest();
//        req.setCurrentPassword("old-raw");
//        req.setNewPassword("new-raw");
//
//        when(currentTenant.requireTenantId()).thenReturn(tid);
//        when(userRepo.findByTenantIdAndUsername(tid, "alice"))
//                .thenReturn(Optional.of(user));
//        when(encoder.matches("old-raw", "old-encoded")).thenReturn(true);
//        when(encoder.encode("new-raw")).thenReturn("new-encoded");
//
//        userService.changePassword("alice", req);
//
//        assertEquals("new-encoded", user.getPassword());
//        assertNotNull(user.getPasswordChangedAt());
//        assertTrue(user.getPasswordChangedAt().isBefore(Instant.now().plusSeconds(1)));
//    }
//
//    @Test
//    void changePassword_wrongCurrentPassword_throwsBadRequest() {
//        UUID tid = UUID.randomUUID();
//        User user = new User();
//        user.setUsername("alice");
//        user.setPassword("old-encoded");
//
//        ChangePasswordRequest req = new ChangePasswordRequest();
//        req.setCurrentPassword("wrong");
//        req.setNewPassword("new-raw");
//
//        when(currentTenant.requireTenantId()).thenReturn(tid);
//        when(userRepo.findByTenantIdAndUsername(tid, "alice"))
//                .thenReturn(Optional.of(user));
//        when(encoder.matches("wrong", "old-encoded")).thenReturn(false);
//
//        ResponseStatusException ex = assertThrows(
//                ResponseStatusException.class,
//                () -> userService.changePassword("alice", req)
//        );
//        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
//    }
//
//    @Test
//    void findByUsername_success() {
//        when(currentTenant.requireTenantId()).thenReturn(tenantId);
//        User user = new User();
//        user.setUsername("bob");
//
//        when(userRepo.findByTenantIdAndUsername(tenantId, "bob"))
//                .thenReturn(Optional.of(user));
//
//        Optional<User> result = userService.findByUsername("bob");
//        assertTrue(result.isPresent());
//        assertEquals("bob", result.get().getUsername());
//    }
//}

