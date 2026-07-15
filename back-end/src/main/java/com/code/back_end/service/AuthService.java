package com.code.back_end.service;

import com.code.back_end.entity.User;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.util.JwtUtil;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService
        implements UserDetailsService {

    private final UserRepository repo;  
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository repo,
            PasswordEncoder encoder,
            JwtUtil jwt,
            AuditLogService auditLogService
    ) {

        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.auditLogService = auditLogService;
    }

@Override
public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {

    User user = repo.findByUsername(username)
            .orElseThrow(() ->
                    new UsernameNotFoundException("User not found")
            );

    return org.springframework.security.core.userdetails.User
            .builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities("ROLE_" + user.getRole())
            .build();
}

    // =========================
    // REGISTER
    // =========================

    public Map<String, Object> register(
            User user
    ) {

        if (
                repo.findByUsername(
                        user.getUsername()
                ).isPresent()
        ) {

            throw new RuntimeException(
                    "Username already exists"
            );
        }

        user.setPassword(
                encoder.encode(
                        user.getPassword()
                )
        );

        // default role
        user.setRole(
                "STAKEHOLDER"
        );

        User savedUser =
                repo.save(user);

        Map<String, Object>
                response =
                new HashMap<>();

        response.put(
                "message",
                "Registered Successfully"
        );

        response.put(
                "id",
                savedUser.getId()
        );

        response.put(
                "username",
                savedUser.getUsername()
        );

        return response;
    }

    // =========================
    // LOGIN
    // =========================

    public Map<String, Object> login(
            String username,
            String password
    ) {

        User user =
                repo.findByUsername(
                        username
                ).orElseThrow(() -> {
                    auditLogService.logAs(
                            null,
                            "ANONYMOUS",
                            "LOGIN_FAILED",
                            "User",
                            null,
                            "Unknown username: " + username
                    );

                    return new RuntimeException(
                            "User not found"
                    );
                });

        if (
                !encoder.matches(
                        password,
                        user.getPassword()
                )
        ) {

            auditLogService.logAs(
                    user,
                    user.getRole(),
                    "LOGIN_FAILED",
                    "User",
                    user.getId(),
                    "Invalid password"
            );

            throw new RuntimeException(
                    "Invalid credentials"
            );
        }

        String token =
                jwt.generateToken(
                        username,
                        user.getRole()
                );

        Map<String, Object>
                response =
                new HashMap<>();

        response.put(
                "token",
                token
        );

        response.put(
                "role",
                user.getRole()
        );

        response.put(
                "username",
                user.getUsername()
        );

        response.put(
                "userId",
                user.getId()
        );

        response.put(
                "id",
                user.getId()
        );

        auditLogService.logAs(
                user,
                user.getRole(),
                "LOGIN_SUCCESS",
                "User",
                user.getId(),
                "User logged in"
        );

        return response;
    }

    // =========================
    // CURRENT USER
    // =========================

    public User getCurrentUser(
            String username
    ) {

        return repo.findByUsername(
                username
        ).orElseThrow(() ->

                new RuntimeException(
                        "User not found"
                )
        );
    }

    // =========================
    // UPDATE ROLE
    // =========================

    public String updateRole(
            Long id,
            String role
    ) {

        User user =
                repo.findById(id)
                        .orElseThrow(() ->

                                new RuntimeException(
                                        "User not found"
                                )
                        );

        user.setRole(role);

        repo.save(user);

        return "Role updated successfully";
    }
}
