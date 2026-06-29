package com.wesleyedwards.ServiceLink.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UserRequestDto(
        @Valid
        @NotNull(message = "Credentials are required")
        CredentialsRequestDto credentials,

        @Valid
        @NotNull(message = "Profile is required")
        ProfileRequestDto profile
) {}
