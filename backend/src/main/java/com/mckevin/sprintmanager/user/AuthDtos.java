package com.mckevin.sprintmanager.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 2, max = 80) String fullName,
            @NotBlank @Size(min = 8, max = 72, message = "doit contenir entre 8 et 72 caractères") String password) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, Instant expiresAt, UserView user) {
    }
}
