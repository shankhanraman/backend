package com.arogya.cafe.security.entity;

import com.arogya.cafe.common.entity.BaseEntity;
import com.arogya.cafe.common.enums.StaffRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * An employee. Doubles as the security principal: username + passwordHash back login,
 * and role maps to a Spring Security authority (ROLE_CHEF, ROLE_CASHIER, ...).
 */
@Entity
@Table(name = "staff")
public class Staff extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StaffRole role;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    protected Staff() {}

    public Staff(String name, StaffRole role, String username, String passwordHash) {
        this.name = name;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StaffRole getRole() {
        return role;
    }

    public void setRole(StaffRole role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
