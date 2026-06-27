package com.fofoqueiro.auth.controller;

import com.fofoqueiro.auth.dto.request.LoginRequest;
import com.fofoqueiro.auth.dto.request.RefreshRequest;
import com.fofoqueiro.auth.dto.request.SelectOrgRequest;
import com.fofoqueiro.auth.dto.request.TotpVerifyRequest;
import com.fofoqueiro.auth.dto.response.AuthResponse;
import com.fofoqueiro.auth.dto.response.TotpSetupResponse;
import com.fofoqueiro.auth.dto.response.UserResponse;
import com.fofoqueiro.auth.security.OrgContext;
import com.fofoqueiro.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Seleção de organização quando usuário pertence a múltiplas orgs */
    @PostMapping("/select-org")
    public ResponseEntity<AuthResponse> selectOrg(@Valid @RequestBody SelectOrgRequest req) {
        return ResponseEntity.ok(authService.selectOrg(req));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<AuthResponse> verifyTotp(@Valid @RequestBody TotpVerifyRequest req) {
        return ResponseEntity.ok(authService.verifyTotp(req));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(@AuthenticationPrincipal String userId) {
        UUID orgId = OrgContext.get();
        return ResponseEntity.ok(authService.setupTotp(UUID.fromString(userId), orgId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       @AuthenticationPrincipal String userId) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7), OrgContext.get());
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.getMe(UUID.fromString(userId), OrgContext.get()));
    }
}
