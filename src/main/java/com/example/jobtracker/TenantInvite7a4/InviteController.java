package com.example.jobtracker.TenantInvite7a4;


import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.*;
import com.example.jobtracker.TenantInvite7a4.TenantAdminService;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
    private final TenantAdminService service;
    public InviteController(TenantAdminService service) {
        this.service = service;
    }
    @GetMapping("/{token}")
    @PermitAll
    public InviteInfoResp preview(@PathVariable String token) {
        return service.previewInvite(token);
    }

    @PostMapping("/{token}/accept")
    @PermitAll
    public void accept(@PathVariable String token, @RequestBody AcceptInviteReq req) {
        service.acceptInvite(token, req);
    }
}