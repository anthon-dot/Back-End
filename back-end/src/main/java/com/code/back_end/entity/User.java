package com.code.back_end.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(unique = true)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 80)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 120)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String role;

    private String status = "ACTIVE";

    private String resetToken;

    private LocalDateTime createdAt =
            LocalDateTime.now();

    // ======================
    // GETTERS & SETTERS
    // ======================

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(
            String username
    ) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(
            String password
    ) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(
            String role
    ) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(
            String resetToken
    ) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
