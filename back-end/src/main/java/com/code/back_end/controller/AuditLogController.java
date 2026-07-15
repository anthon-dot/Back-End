package com.code.back_end.controller;

import com.code.back_end.entity.AuditLog;
import com.code.back_end.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(
            AuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<AuditLog> getAll() {
        return auditLogService.getAll();
    }
}
