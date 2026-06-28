package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.config.JwtAuthFilter;
import com.wesleyedwards.ServiceLink.config.SecurityConfig;
import com.wesleyedwards.ServiceLink.dtos.CredentialsRequestDto;
import com.wesleyedwards.ServiceLink.dtos.ProfileRequestDto;
import com.wesleyedwards.ServiceLink.dtos.UserIdResponseDto;
import com.wesleyedwards.ServiceLink.dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.exceptions.BadRequestException;
import com.wesleyedwards.ServiceLink.exceptions.NotFoundException;
import com.wesleyedwards.ServiceLink.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;

    private UserResponseDto sampleUser(UUID id) {
        return new UserResponseDto(id, null, List.of(), List.of());
    }

    @Test
    @DisplayName("POST /api/users/auth/register returns 201 with the created user")
    void register_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.createUser(any(UserRequestDto.class))).thenReturn(sampleUser(id));

        String body = """
                {
                  "credentials": {"username": "jdoe", "password": "raw-pw"},
                  "profile": {"firstName": "John", "lastName": "Doe", "email": "john.doe@example.com"}
                }
                """;

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(id.toString()));

        verify(userService).createUser(any(UserRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/users/auth/login returns 200 with a token")
    void login_returns200WithToken() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.login(any(CredentialsRequestDto.class)))
                .thenReturn(new UserIdResponseDto(id, "jwt-token", Role.ADMIN));

        String body = "{\"username\": \"jdoe\", \"password\": \"raw-pw\"}";

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id.toString()))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/users/auth/login returns 400 with the message when the password is wrong")
    void login_invalidPassword_returns400() throws Exception {
        when(userService.login(any(CredentialsRequestDto.class)))
                .thenThrow(new BadRequestException("Invalid password"));

        String body = "{\"username\": \"jdoe\", \"password\": \"wrong\"}";

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid password"));
    }

    @Test
    @DisplayName("POST /api/users/auth/login returns 404 when the user does not exist")
    void login_unknownUser_returns404() throws Exception {
        when(userService.login(any(CredentialsRequestDto.class)))
                .thenThrow(new NotFoundException("User: ghost does not exist"));

        String body = "{\"username\": \"ghost\", \"password\": \"raw-pw\"}";

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User: ghost does not exist"));
    }

    @Test
    @DisplayName("GET /api/users returns 200 with the user list")
    void getAllUsers_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getAllUsers()).thenReturn(List.of(sampleUser(id)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(id.toString()));
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 200 and queries the right id")
    void getUser_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUser(eq(id))).thenReturn(sampleUser(id));

        mockMvc.perform(get("/api/users/{userId}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id.toString()));

        verify(userService).getUser(id);
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 404 when the user is missing")
    void getUser_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUser(eq(id)))
                .thenThrow(new NotFoundException("User: " + id + " does not exist"));

        mockMvc.perform(get("/api/users/{userId}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User: " + id + " does not exist"));
    }

    @Test
    @DisplayName("PATCH /api/users/profile/{id} returns 200 and forwards the profile changes")
    void updateUser_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.updateUser(eq(id), any(ProfileRequestDto.class))).thenReturn(sampleUser(id));

        String body = "{\"firstName\": \"Jane\", \"lastName\": \"Doe\", \"email\": \"jane.doe@example.com\"}";

        mockMvc.perform(patch("/api/users/profile/{userId}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(id), any(ProfileRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} returns 204 and triggers a soft delete")
    void deleteUser_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}", id))
                .andExpect(status().isNoContent());

        verify(userService).deleteuser(id);
    }
}
