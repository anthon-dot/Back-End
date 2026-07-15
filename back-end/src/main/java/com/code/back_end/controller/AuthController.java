package com.code.back_end.controller;

import com.code.back_end.dto.LoginRequest;
import com.code.back_end.entity.User;
import com.code.back_end.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final AuthService service;

    public AuthController(
            AuthService service
    ) {

        this.service = service;
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public Map<String, Object> register(
            @Valid @RequestBody User user
    ) {

        return service.register(user);
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return service.login(
                request.username,
                request.password
        );
    }

    // =========================
    // GET CURRENT USER
    // =========================


    @GetMapping("/me")
    public User me(
            Authentication authentication
    ) {

        return service.getCurrentUser(
                authentication.getName()
        );
    }

    // =========================
    // UPDATE ROLE
    // =========================

    @PutMapping("/update-role/{id}")
    public String updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {

        return service.updateRole(
                id,
                request.get("role")
        );
    }
}
