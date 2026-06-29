package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.enums.Role;
import jakarta.validation.constraints.NotNull;

public record RoleRequestDto(
        @NotNull(message = "Role is required")
        Role role
) {}
