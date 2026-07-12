package com.wesleyedwards.ServiceLink.dtos;

import jakarta.validation.constraints.Email;

/**
 * Partial-update DTO for PATCH /api/users/profile/{userId}.
 * All fields are optional; only the format constraint on email applies,
 * and {@code @Email} is null-tolerant so omitted fields are left unchanged.
 */
public record ProfileUpdateDto(
        String firstName,
        String lastName,

        @Email(message = "Email must be valid")
        String email
) {}
