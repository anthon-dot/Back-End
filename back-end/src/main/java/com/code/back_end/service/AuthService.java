package com.code.back_end.service;

import com.code.back_end.entity.User;
import com.code.back_end.exception.DuplicateResourceException;
import com.code.back_end.exception.ResourceNotFoundException;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService implements UserDetailsService {

    private static final String INVALID_CREDENTIALS_MSG = "Invalid username or password";

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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MSG));

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
    public Map<String, Object> register(User user) {
        if (repo.findByUsername(user.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Username already exists");
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("STAKEHOLDER");

        User savedUser = repo.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registered Successfully");
        response.put("id", savedUser.getId());
        response.put("username", savedUser.getUsername());

        return response;
    }

    // =========================
    // LOGIN (Unified Error to Prevent User Enumeration)
    // =========================
    public Map<String, Object> login(String username, String password) {
        Optional<User> optionalUser = repo.findByUsername(username);

        if (optionalUser.isEmpty()) {
            auditLogService.logAs(
                    null,
                    "ANONYMOUS",
                    "LOGIN_FAILED",
                    "User",
                    null,
                    "Login failed for username: " + username
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }

        User user = optionalUser.get();

        if (!encoder.matches(password, user.getPassword())) {
            auditLogService.logAs(
                    user,
                    user.getRole(),
                    "LOGIN_FAILED",
                    "User",
                    user.getId(),
                    "Login failed: invalid password"
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MSG);
        }

        String token = jwt.generateToken(username, user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("username", user.getUsername());
        response.put("userId", user.getId());
        response.put("id", user.getId());

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
    public User getCurrentUser(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // =========================
    // UPDATE ROLE
    // =========================
    public String updateRole(Long id, String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role cannot be empty");
        }

        User user = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setRole(role);
        repo.save(user);

        return "Role updated successfully";
    }
}
