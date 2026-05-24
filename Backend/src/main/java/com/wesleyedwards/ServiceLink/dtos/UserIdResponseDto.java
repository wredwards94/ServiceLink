package com.wesleyedwards.ServiceLink.dtos;

import com.wesleyedwards.ServiceLink.enums.Role;

import java.util.UUID;

public record UserIdResponseDto(
        UUID userId,
        String token,
        Role role
) {}
