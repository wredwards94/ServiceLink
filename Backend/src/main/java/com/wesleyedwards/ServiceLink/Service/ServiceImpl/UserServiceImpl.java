package com.wesleyedwards.ServiceLink.Service.ServiceImpl;

import com.wesleyedwards.ServiceLink.Dtos.CredentialsRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.UserIdResponseDto;
import com.wesleyedwards.ServiceLink.Dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.Entities.User;
import com.wesleyedwards.ServiceLink.Exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.Exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.Mappers.CredentialsMapper;
import com.wesleyedwards.ServiceLink.Mappers.ProfileMapper;
import com.wesleyedwards.ServiceLink.Mappers.UserMapper;
import com.wesleyedwards.ServiceLink.Repositories.UserRepository;
import com.wesleyedwards.ServiceLink.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CredentialsMapper credentialsMapper;
    private final ProfileMapper profileMapper;

    @Override
    public UserResponseDto createUser(UserRequestDto newUser) {
        User user = userMapper.requestDtoToEntity(newUser);
        userRepository.saveAndFlush(user);

        return userMapper.entityToResponseDto(user);
    }

    @Override
    public UserIdResponseDto login(CredentialsRequestDto credentials) {
        Optional<User> optionalUser =
                userRepository.findByCredentialsUsername(credentials.username());

        if(optionalUser.isEmpty()) throw new NotFoundException("user: " + credentials.username() + " does not exist");
        if(!optionalUser.get().getCredentials().getPassword().equals(credentials.password())) throw new BadRequestException("Invalid password");

        return userMapper.entityToIdResponseDto(optionalUser.get());
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userMapper.entitiesToResponseDtos(userRepository.findAll());
    }
}
