package com.example.investsmart.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    private String role;
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    private boolean emailVerified;
    @Column(name = "password_reset_required", nullable = false)
    private boolean passwordResetRequired;
    private String accountStatus = "ACTIVE";
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean value) { emailVerified = value; }
    public boolean isPasswordResetRequired() { return passwordResetRequired; }
    public void setPasswordResetRequired(boolean value) { passwordResetRequired = value; }
    public String getAccountStatus() { return accountStatus == null ? "ACTIVE" : accountStatus; }
    public void setAccountStatus(String value) { accountStatus = value; }
    public void requireActive() {
        if (!"ACTIVE".equals(getAccountStatus())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"This account is suspended. Contact support.");
    }
    private BigDecimal virtualBalance;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }

    public User() {
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public BigDecimal getVirtualBalance() {
        return virtualBalance;
    }

    public void setVirtualBalance(BigDecimal virtualBalance) {
        this.virtualBalance = virtualBalance;
    }

    // getters and setters
}
