package com.example.jobtracker.TenantInvite7a4;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.example.jobtracker.TenantInvite7a4.TenantAdminService;


    @RestController
    // 修改为（管理员专属前缀，避免冲突）// 修改为（管理员专属前缀，避免冲突）
    @RequestMapping("/api/admin/tenants")
    public class TenantAdminController {

        private final TenantAdminService tenantAdminService;

        public TenantAdminController(TenantAdminService tenantAdminService) {
            this.tenantAdminService = tenantAdminService;
        }

        // 创建邀请（仅租户/系统管理员）
        @PostMapping("/{tenantId}/invites")
        @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
        public InviteInfoResp createInvite(@PathVariable UUID tenantId, @RequestBody CreateInviteReq req) {
            return tenantAdminService.createInvite(tenantId, req);
        }

        // 成员列表
        @GetMapping("/{tenantId}/members")
        @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
        public List<MemberDto> members(@PathVariable UUID tenantId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
     return tenantAdminService.listMembers(tenantId, page, size).getContent();
        }

        // 删除成员
        @DeleteMapping("/{tenantId}/members/{userId}")
        @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
        public void remove(@PathVariable UUID tenantId, @PathVariable UUID userId) {
            tenantAdminService.removeMember(tenantId, userId);
        }
    }

