package com.wesleyedwards.ServiceLink.service.serviceimpl;

import com.wesleyedwards.ServiceLink.config.JwtUtil;
import com.wesleyedwards.ServiceLink.config.UserPrincipal;
import com.wesleyedwards.ServiceLink.dtos.CredentialsRequestDto;
import com.wesleyedwards.ServiceLink.dtos.ProfileRequestDto;
import com.wesleyedwards.ServiceLink.dtos.UserIdResponseDto;
import com.wesleyedwards.ServiceLink.dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.entities.Credentials;
import com.wesleyedwards.ServiceLink.entities.Profile;
import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.mappers.ProfileMapper;
import com.wesleyedwards.ServiceLink.mappers.UserMapper;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private ProfileMapper profileMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private UserServiceImpl userService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setUserId(userId);
        user.setCredentials(new Credentials("jdoe", "hashed-pw"));
        user.setProfile(new Profile("John", "Doe", "john.doe@example.com"));
        user.setRole(Role.USER);
    }

    @Test
    @DisplayName("createUser encodes the raw password before saving")
    void createUser_encodesPassword() {
        UserRequestDto request = new UserRequestDto(
                new CredentialsRequestDto("jdoe", "raw-pw"),
                new ProfileRequestDto("John", "Doe", "john.doe@example.com"));

        User mapped = new User();
        mapped.setCredentials(new Credentials("jdoe", "raw-pw"));
        UserResponseDto expected = new UserResponseDto(userId, null, List.of(), List.of());

        when(userMapper.requestDtoToEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode("raw-pw")).thenReturn("hashed-pw");
        when(userMapper.entityToResponseDto(mapped)).thenReturn(expected);

        UserResponseDto result = userService.createUser(request);

        assertSame(expected, result);
        assertEquals("hashed-pw", mapped.getCredentials().getPassword());
        verify(passwordEncoder).encode("raw-pw");
        verify(userRepository).saveAndFlush(mapped);
    }

    @Test
    @DisplayName("login returns a token for the authenticated principal")
    void login_success() {
        CredentialsRequestDto creds = new CredentialsRequestDto("jdoe", "raw-pw");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(user), null, List.of());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("jdoe", Role.USER)).thenReturn("jwt-token");

        UserIdResponseDto result = userService.login(creds);

        assertEquals(userId, result.userId());
        assertEquals("jwt-token", result.token());
        assertEquals(Role.USER, result.role());
    }

    @Test
    @DisplayName("login propagates BadCredentialsException for invalid credentials")
    void login_invalidCredentials() {
        CredentialsRequestDto creds = new CredentialsRequestDto("jdoe", "wrong-pw");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> userService.login(creds));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login propagates DisabledException for a disabled account")
    void login_disabledAccount() {
        CredentialsRequestDto creds = new CredentialsRequestDto("jdoe", "raw-pw");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account is disabled"));

        assertThrows(DisabledException.class, () -> userService.login(creds));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("getAllUsers delegates to the repository and mapper")
    void getAllUsers_delegates() {
        List<User> users = List.of(user);
        List<UserResponseDto> expected = List.of(new UserResponseDto(userId, null, List.of(), List.of()));

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.entitiesToResponseDtos(users)).thenReturn(expected);

        assertSame(expected, userService.getAllUsers());
    }

    @Test
    @DisplayName("getUser returns the mapped user when found")
    void getUser_found() {
        UserResponseDto expected = new UserResponseDto(userId, null, List.of(), List.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.entityToResponseDto(user)).thenReturn(expected);

        assertSame(expected, userService.getUser(userId));
    }

    @Test
    @DisplayName("getUser throws NotFoundException when missing")
    void getUser_missing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUser(userId));
    }

    @Test
    @DisplayName("updateUser applies the profile changes and saves")
    void updateUser_updatesProfile() {
        ProfileRequestDto update = new ProfileRequestDto("Jane", "Doe", "jane.doe@example.com");
        UserResponseDto expected = new UserResponseDto(userId, null, List.of(), List.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.entityToResponseDto(user)).thenReturn(expected);

        assertSame(expected, userService.updateUser(userId, update));
        verify(profileMapper).updateProfileFromDto(update, user.getProfile());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteuser performs a soft delete by disabling the user")
    void deleteUser_softDeletes() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteuser(userId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertTrue(captor.getValue().isDisabled());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteuser throws NotFoundException when the user is missing")
    void deleteUser_missing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.deleteuser(userId));
        verify(userRepository, never()).saveAndFlush(any());
    }
}
