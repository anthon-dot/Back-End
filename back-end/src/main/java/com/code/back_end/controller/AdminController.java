package com.code.back_end.controller;

import com.code.back_end.entity.*;
import com.code.back_end.exception.ResourceNotFoundException;
import com.code.back_end.repository.*;
import com.code.back_end.security.SecurityService;
import com.code.back_end.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin-only controller.
 * Every endpoint calls securityService.requireAdmin() — the SecurityConfig
 * also enforces ROLE_ADMIN at the HTTP layer for /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // Characters used when generating a temporary password
    private static final String TEMP_PW_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#";
    private static final int TEMP_PW_LENGTH = 10;

    private final SecurityService      securityService;
    private final UserRepository       userRepository;
    private final AuditLogRepository   auditLogRepository;
    private final RentalRateService    rentalRateService;
    private final StallTypeService     stallTypeService;
    private final SystemSettingsService systemSettingsService;
    private final PasswordEncoder      passwordEncoder;
    private final AuditLogService      auditLogService;

    public AdminController(
            SecurityService securityService,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            RentalRateService rentalRateService,
            StallTypeService stallTypeService,
            SystemSettingsService systemSettingsService,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.securityService       = securityService;
        this.userRepository        = userRepository;
        this.auditLogRepository    = auditLogRepository;
        this.rentalRateService     = rentalRateService;
        this.stallTypeService      = stallTypeService;
        this.systemSettingsService = systemSettingsService;
        this.passwordEncoder       = passwordEncoder;
        this.auditLogService       = auditLogService;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    /** List all users. Password hash is never returned (JsonProperty.WRITE_ONLY on User). */
    @GetMapping("/users")
    public List<Map<String, Object>> getUsers() {
        securityService.requireAdmin();
        return userRepository.findAll().stream()
                .map(this::toUserMap)
                .collect(Collectors.toList());
    }

    /** Update a user's role, status, email, contact — NOT password. */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload
    ) {
        securityService.requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (payload.containsKey("role") && payload.get("role") != null) {
            user.setRole(String.valueOf(payload.get("role")));
        }
        if (payload.containsKey("status") && payload.get("status") != null) {
            user.setStatus(String.valueOf(payload.get("status")));
        }

        userRepository.save(user);
        auditLogService.log("UPDATE_USER", "User", id, "Admin updated user: " + user.getUsername());
        return toUserMap(user);
    }

    /** Activate a user account (set status = ACTIVE). */
    @PutMapping("/users/{id}/activate")
    public Map<String, Object> activateUser(@PathVariable Long id) {
        securityService.requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setStatus("ACTIVE");
        userRepository.save(user);
        auditLogService.log("ACTIVATE_USER", "User", id, "Admin activated user: " + user.getUsername());
        return toUserMap(user);
    }

    /** Disable a user account (set status = INACTIVE). */
    @PutMapping("/users/{id}/disable")
    public Map<String, Object> disableUser(@PathVariable Long id) {
        securityService.requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setStatus("INACTIVE");
        userRepository.save(user);
        auditLogService.log("DISABLE_USER", "User", id, "Admin disabled user: " + user.getUsername());
        return toUserMap(user);
    }

    /**
     * Reset a user's password.
     * Generates a random 10-character temporary password, BCrypt-hashes it,
     * and returns the plaintext so the admin can hand it to the user.
     */
    @PutMapping("/users/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id) {
        securityService.requireAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        auditLogService.log("RESET_PASSWORD", "User", id, "Admin reset password for: " + user.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Password reset successfully.");
        result.put("temporaryPassword", tempPassword);
        result.put("username", user.getUsername());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN HISTORY  (derived from audit_logs where action is LOGIN_*)
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/login-history")
    public List<Map<String, Object>> getLoginHistory() {
        securityService.requireAdmin();
        return auditLogRepository.findAll().stream()
                .filter(log -> log.getAction() != null &&
                        (log.getAction().startsWith("LOGIN_SUCCESS") ||
                         log.getAction().startsWith("LOGIN_FAILED")))
                .sorted(Comparator.comparing(
                        AuditLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id",        log.getId());
                    row.put("username",  log.getUser() != null ? log.getUser().getUsername() : "");
                    row.put("user",      log.getUser() != null ? log.getUser().getUsername() : "");
                    row.put("role",      log.getRole());
                    row.put("status",    log.getAction().contains("FAILED") ? "FAILED" : "SUCCESS");
                    row.put("createdAt", log.getCreatedAt());
                    row.put("details",   log.getDetails());
                    return row;
                })
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RENTAL RATES
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/rental-rates")
    public List<RentalRate> getRentalRates() {
        return rentalRateService.getAll();
    }

    @PostMapping("/rental-rates")
    public RentalRate createRentalRate(@RequestBody RentalRate rate) {
        RentalRate saved = rentalRateService.create(rate);
        auditLogService.log("CREATE_RENTAL_RATE", "RentalRate", saved.getId(),
                "Created rental rate for stall type: " + saved.getStallType());
        return saved;
    }

    @PutMapping("/rental-rates/{id}")
    public RentalRate updateRentalRate(@PathVariable Long id, @RequestBody RentalRate rate) {
        RentalRate updated = rentalRateService.update(id, rate);
        auditLogService.log("UPDATE_RENTAL_RATE", "RentalRate", id,
                "Updated rental rate: " + updated.getStallType());
        return updated;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STALL TYPES
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/stall-types")
    public List<StallType> getStallTypes() {
        return stallTypeService.getAll();
    }

    @PostMapping("/stall-types")
    public StallType createStallType(@RequestBody StallType type) {
        StallType saved = stallTypeService.create(type);
        auditLogService.log("CREATE_STALL_TYPE", "StallType", saved.getId(),
                "Created stall type: " + saved.getName());
        return saved;
    }

    @PutMapping("/stall-types/{id}")
    public StallType updateStallType(@PathVariable Long id, @RequestBody StallType type) {
        StallType updated = stallTypeService.update(id, type);
        auditLogService.log("UPDATE_STALL_TYPE", "StallType", id,
                "Updated stall type: " + updated.getName());
        return updated;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SYSTEM SETTINGS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/settings")
    public SystemSettings getSettings() {
        return systemSettingsService.getOrCreate();
    }

    @PutMapping("/settings")
    public SystemSettings updateSettings(@RequestBody SystemSettings payload) {
        SystemSettings updated = systemSettingsService.update(payload);
        auditLogService.log("UPDATE_SETTINGS", "SystemSettings", 1L, "Admin updated system settings");
        return updated;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Safe user projection — never includes the password hash. */
    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id",        user.getId());
        map.put("username",  user.getUsername());
        map.put("role",      user.getRole());
        map.put("status",    user.getStatus() != null ? user.getStatus() : "ACTIVE");
        map.put("createdAt", user.getCreatedAt());
        // password intentionally omitted
        return map;
    }

    private String generateTempPassword() {
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder(TEMP_PW_LENGTH);
        for (int i = 0; i < TEMP_PW_LENGTH; i++) {
            sb.append(TEMP_PW_CHARS.charAt(rng.nextInt(TEMP_PW_CHARS.length())));
        }
        return sb.toString();
    }
}
