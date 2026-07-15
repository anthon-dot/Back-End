package com.code.back_end.service;

import com.code.back_end.entity.AuditLog;
import com.code.back_end.entity.User;
import com.code.back_end.repository.AuditLogRepository;
import com.code.back_end.security.SecurityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityService securityService;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            SecurityService securityService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.securityService = securityService;
    }

    public List<AuditLog> getAll() {
        securityService.requireAdmin();
        return auditLogRepository.findAll();
    }

    public void log(
            String action,
            String entityName,
            Long entityId,
            String details
    ) {

        User user;

        try {
            user = securityService.currentUser();
        } catch (RuntimeException error) {
            user = null;
        }

        logAs(
                user,
                user != null
                        ? user.getRole()
                        : "ANONYMOUS",
                action,
                entityName,
                entityId,
                details
        );
    }

    public void logAs(
            User user,
            String role,
            String action,
            String entityName,
            Long entityId,
            String details
    ) {

        AuditLog auditLog =
                new AuditLog();

        auditLog.setUser(user);
        auditLog.setRole(role);
        auditLog.setAction(action);
        auditLog.setEntityName(entityName);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);

        auditLogRepository.save(auditLog);
    }
}
