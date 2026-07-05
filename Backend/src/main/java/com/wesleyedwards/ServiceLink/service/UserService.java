package com.wesleyedwards.ServiceLink.service;

import com.wesleyedwards.ServiceLink.dtos.*;
import com.wesleyedwards.ServiceLink.enums.Role;
import jakarta.validation.Valid;

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

    void changePassword(UUID userId, @Valid ChangePasswordRequestDto dto);

    void forgotPassword(@Valid ForgotPasswordDto dto);

    void resetPassword(@Valid ResetPasswordDto dto);

    void setUserStatus(UUID id, StatusRequestDto statusDto);
}
