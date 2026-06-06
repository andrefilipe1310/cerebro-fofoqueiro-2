package com.fofoqueiro.auth.dto.request;

import com.fofoqueiro.auth.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserRole role
) {}
