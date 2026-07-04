package com.wesleyedwards.ServiceLink.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDto(@NotBlank String token,
                               @NotBlank @Size(min = 8) String newPassword) {}
