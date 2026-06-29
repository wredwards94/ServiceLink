package com.wesleyedwards.ServiceLink.controllers;

import com.wesleyedwards.ServiceLink.dtos.*;
import com.wesleyedwards.ServiceLink.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins="*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto newUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(newUser));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserIdResponseDto> login(@Valid @RequestBody CredentialsRequestDto credentials) {
        return ResponseEntity.ok(userService.login(credentials));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PatchMapping("/profile/{userId}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID userId, @Valid @RequestBody ProfileUpdateDto updateProf) {
        return ResponseEntity.ok(userService.updateUser(userId, updateProf));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<UserResponseDto> deleteUser(@PathVariable UUID userId) {
        userService.deleteuser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponseDto> updateRole(@PathVariable UUID userId, @Valid @RequestBody RoleRequestDto request) {
        return ResponseEntity.ok(userService.updateUserRole(userId, request.role()));
    }
}
