package com.code.back_end.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import com.code.back_end.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuditLogService auditLogService;

    public GlobalExceptionHandler(
            AuditLogService auditLogService
    ) {
        this.auditLogService = auditLogService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException error
    ) {

        Map<String, String> fields =
                new LinkedHashMap<>();

        for (FieldError fieldError :
                error.getBindingResult().getFieldErrors()) {
            fields.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return ResponseEntity
                .badRequest()
                .body(body(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        fields
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException error
    ) {

        return ResponseEntity
                .badRequest()
                .body(body(
                        HttpStatus.BAD_REQUEST,
                        error.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException error
    ) {

        auditLogService.log(
                "ACCESS_DENIED",
                "Security",
                null,
                error.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(body(
                        HttpStatus.FORBIDDEN,
                        error.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException error
    ) {

        HttpStatus status =
                HttpStatus.valueOf(
                        error.getStatusCode().value()
                );

        return ResponseEntity
                .status(status)
                .body(body(
                        status,
                        error.getReason(),
                        null
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException error
    ) {

        return ResponseEntity
                .badRequest()
                .body(body(
                        HttpStatus.BAD_REQUEST,
                        error.getMessage(),
                        null
                ));
    }

    private Map<String, Object> body(
            HttpStatus status,
            String message,
            Object errors
    ) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                LocalDateTime.now()
        );
        body.put(
                "status",
                status.value()
        );
        body.put(
                "error",
                status.getReasonPhrase()
        );
        body.put(
                "message",
                message
        );

        if (errors != null) {
            body.put(
                    "errors",
                    errors
            );
        }

        return body;
    }
}
