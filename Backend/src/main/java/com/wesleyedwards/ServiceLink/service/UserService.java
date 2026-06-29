package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.dtos.*;
import com.wesleyedwards.ServiceLink.enums.Role;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDto createUser(UserRequestDto newUser);

    UserIdResponseDto login(CredentialsRequestDto credentials);

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUser(UUID userId);

    UserResponseDto updateUser(UUID userId, ProfileUpdateDto updateProf);

    UserResponseDto updateUserRole(UUID userId, Role role);

    void deleteuser(UUID userId);
}
