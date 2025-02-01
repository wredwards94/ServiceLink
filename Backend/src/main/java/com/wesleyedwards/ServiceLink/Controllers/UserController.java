package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Dtos.CredentialsRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.UserIdResponseDto;
import com.wesleyedwards.ServiceLink.Dtos.UserRequestDto;
import com.wesleyedwards.ServiceLink.Dtos.UserResponseDto;
import com.wesleyedwards.ServiceLink.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins="*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserRequestDto newUser) {
        return ResponseEntity.ok(userService.createUser(newUser));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserIdResponseDto> login(@RequestBody CredentialsRequestDto credentials) {
        return ResponseEntity.ok(userService.login(credentials));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
