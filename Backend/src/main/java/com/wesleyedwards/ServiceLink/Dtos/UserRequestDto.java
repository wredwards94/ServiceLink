package com.wesleyedwards.ServiceLink.Dtos;

import jakarta.validation.constraints.NotNull;

public record UserRequestDto(
        @NotNull(message = "Credentials are required")
        CredentialsRequestDto credentials,

        @NotNull(message = "Profile is required")
        ProfileRequestDto profile
) {}
