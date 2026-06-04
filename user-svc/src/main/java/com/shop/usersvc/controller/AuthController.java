package com.shop.usersvc.controller;

import com.shop.usersvc.dto.AuthResponse;
import com.shop.usersvc.dto.LoginRequest;
import com.shop.usersvc.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
