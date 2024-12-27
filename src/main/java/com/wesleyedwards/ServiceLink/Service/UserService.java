package com.wesleyedwards.ServiceLink.Service;

import com.wesleyedwards.ServiceLink.Dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.Entities.Credentials;
import com.wesleyedwards.ServiceLink.Entities.User;

import java.util.List;

public interface UserService {
    UserResponseDto createUser(UserRequestDto newUser);

    User login(Credentials credentials);

    List<User> getAllUsers();
}
