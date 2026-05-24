package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.dtos.*;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDto createUser(UserRequestDto newUser);

    UserIdResponseDto login(CredentialsRequestDto credentials);

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUser(UUID userId);

    UserResponseDto updateUser(UUID userId, ProfileRequestDto updateProf);

    void deleteuser(UUID userId);
}
