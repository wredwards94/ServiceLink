package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.JwtUtil;
import com.wesleyedwards.ServiceLink.dtos.*;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.CredentialsMapper;
import com.wesleyedwards.ServiceLink.mappers.ProfileMapper;
import com.wesleyedwards.ServiceLink.mappers.UserMapper;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserResponseDto createUser(UserRequestDto newUser) {
        User user = userMapper.requestDtoToEntity(newUser);
        user.getCredentials().setPassword(
                passwordEncoder.encode(user.getCredentials().getPassword())
        );
        userRepository.saveAndFlush(user);
        return userMapper.entityToResponseDto(user);
    }

    @Override
    public UserIdResponseDto login(CredentialsRequestDto credentials) {
        User foundUser = checkUserExistsByUsername(credentials.username());
        if(!passwordEncoder.matches(credentials.password(), foundUser.getCredentials().getPassword()))
            throw new BadRequestException("Invalid password");

        String token = jwtUtil.generateToken(
                foundUser.getCredentials().getUsername(),
                foundUser.getRole()
        );

        return new UserIdResponseDto(foundUser.getUserId(), token, foundUser.getRole());
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userMapper.entitiesToResponseDtos(userRepository.findAll());
    }

    @Override
    public UserResponseDto getUser(UUID userId) {
        return userMapper.entityToResponseDto(checkUserExists(userId));
    }

    @Override
    public UserResponseDto updateUser(UUID userId, ProfileRequestDto updateProf) {
        User foundUser = checkUserExists(userId);

        profileMapper.updateProfileFromDto(updateProf, foundUser.getProfile());

        return userMapper.entityToResponseDto(userRepository.save(foundUser));
    }

    @Override
    public void deleteuser(UUID userId) {
        User foundUser = checkUserExists(userId);

        foundUser.setDisabled(true);
        userRepository.saveAndFlush(foundUser);
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist");

        return optionalUser.get();
    }

    private User checkUserExistsByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByCredentialsUsername(username);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + username + " does not exist");

        return optionalUser.get();
    }
}
