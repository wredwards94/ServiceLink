package com.wesleyedwards.ServiceLink.Controllers;

import com.wesleyedwards.ServiceLink.Entities.Credentials;
import com.wesleyedwards.ServiceLink.Entities.User;
import com.wesleyedwards.ServiceLink.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public User createUser(@RequestBody User newUser) {
        return userService.createUser(newUser);
    }

    @PostMapping("/login")
    public User login(@RequestBody Credentials credentials) {
        return userService.login(credentials);
    }
}
