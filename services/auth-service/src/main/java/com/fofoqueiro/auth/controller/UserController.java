package com.fofoqueiro.auth.controller;

import com.fofoqueiro.auth.dto.request.CreateUserRequest;
import com.fofoqueiro.auth.dto.response.UserResponse;
import com.fofoqueiro.auth.security.OrgContext;
import com.fofoqueiro.auth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers(OrgContext.get()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userManagementService.createUser(OrgContext.get(), req));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeMembership(@PathVariable UUID userId) {
        userManagementService.removeMembership(OrgContext.get(), userId);
        return ResponseEntity.noContent().build();
    }
}
