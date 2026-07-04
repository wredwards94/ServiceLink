package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.JwtUtil;
import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.*;
import com.wesleyedwards.ServiceLink.entities.PasswordResetToken;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.ProfileMapper;
import com.wesleyedwards.ServiceLink.mappers.UserMapper;
import com.wesleyedwards.ServiceLink.repositories.PasswordResetTokenRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserMapper userMapper;
    private final ProfileMapper profileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

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
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        credentials.username(), credentials.password()));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        String token = jwtUtil.generateToken(principal.getUsername(), principal.getRole());
        return new UserIdResponseDto(principal.getUserId(), token, principal.getRole());
    }
//    public UserIdResponseDto login(CredentialsRequestDto credentials) {
//        System.out.println(credentials);
//        User foundUser = checkUserExistsByUsername(credentials.username());
//        if(!passwordEncoder.matches(credentials.password(), foundUser.getCredentials().getPassword()))
//            throw new BadRequestException("Invalid password");
//
//        String token = jwtUtil.generateToken(
//                foundUser.getCredentials().getUsername(),
//                foundUser.getRole()
//        );
//
//        return new UserIdResponseDto(foundUser.getUserId(), token, foundUser.getRole());
//    }

    @Override
    public List<UserResponseDto> getAllUsers() {
        return userMapper.entitiesToResponseDtos(userRepository.findAll());
    }

    @Override
    public UserResponseDto getUser(UUID userId) {
        return userMapper.entityToResponseDto(checkUserExists(userId));
    }

    @Override
    public UserResponseDto updateUser(UUID userId, ProfileUpdateDto updateProf) {
        User foundUser = checkUserExists(userId);

        profileMapper.updateProfileFromDto(updateProf, foundUser.getProfile());

        return userMapper.entityToResponseDto(userRepository.save(foundUser));
    }

    @Override
    public UserResponseDto updateUserRole(UUID userId, Role role) {
        User foundUser = checkUserExists(userId);

        foundUser.setRole(role);

        return userMapper.entityToResponseDto(userRepository.save(foundUser));
    }

    @Override
    public void deleteuser(UUID userId) {
        User foundUser = checkUserExists(userId);

        foundUser.setDisabled(true);
        userRepository.saveAndFlush(foundUser);
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequestDto dto) {
        User foundUser = checkUserExists(userId);

        if(!passwordEncoder.matches(dto.currentPassword(), foundUser.getCredentials().getPassword()))
            throw new BadRequestException("Invalid password");

        foundUser.getCredentials().setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(foundUser);
    }

    @Override
    public void forgotPassword(ForgotPasswordDto dto) {
        userRepository.findByProfileEmail(dto.email()).ifPresent(user -> {
            // create + save token, then log it
            PasswordResetToken token = new PasswordResetToken();
            token.setToken(UUID.randomUUID().toString());
            token.setUser(user);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            passwordResetTokenRepository.save(token);
            log.info("Password reset token for {}: {}", dto.email(), token.getToken());
        });
    }

    private User checkUserExists(UUID userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + userId + " does not exist");

        return optionalUser.get();
    }

//    private User checkUserExistsByUsername(String username) {
//        Optional<User> optionalUser = userRepository.findByCredentialsUsername(username);
//
//        if(optionalUser.isEmpty()) throw new NotFoundException("User: " + username + " does not exist");
//
//        return optionalUser.get();
//    }
}
