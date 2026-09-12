package com.code.back_end.controller;

import com.code.back_end.entity.User;
import com.code.back_end.repository.AuditLogRepository;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.security.SecurityService;
import com.code.back_end.service.AuditLogService;
import com.code.back_end.service.RentalRateService;
import com.code.back_end.service.StallTypeService;
import com.code.back_end.service.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private SecurityService securityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private RentalRateService rentalRateService;
    @Mock
    private StallTypeService stallTypeService;
    @Mock
    private SystemSettingsService systemSettingsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditLogService auditLogService;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(
                securityService,
                userRepository,
                auditLogRepository,
                rentalRateService,
                stallTypeService,
                systemSettingsService,
                passwordEncoder,
                auditLogService
        );
    }

    @Test
    void createUser_success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "market_officer");
        payload.put("name", "Market Officer 1");
        payload.put("role", "MARKET_SUPERVISOR");
        payload.put("password", "Secret123!");
        payload.put("status", "ACTIVE");

        when(userRepository.findByUsername("market_officer")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secret123!")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(99L);
            return u;
        });

        ResponseEntity<Map<String, Object>> response = controller.createUser(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(99L, response.getBody().get("id"));
        assertEquals("market_officer", response.getBody().get("username"));
        assertEquals("Market Officer 1", response.getBody().get("name"));
        assertEquals("MARKET_SUPERVISOR", response.getBody().get("role"));
        assertEquals("ACTIVE", response.getBody().get("status"));

        verify(securityService).requireAdmin();
        verify(auditLogService).log(eq("CREATE_USER"), eq("User"), eq(99L), anyString());
    }

    @Test
    void createUser_roleWithPrefix_stripsPrefix() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "bplo_officer");
        payload.put("role", "ROLE_BPLO");
        payload.put("password", "Secret123!");

        when(userRepository.findByUsername("bplo_officer")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        ResponseEntity<Map<String, Object>> response = controller.createUser(payload);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("BPLO", response.getBody().get("role"));
    }
}
