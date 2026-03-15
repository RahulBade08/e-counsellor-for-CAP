package com.ecounsellor.backend.college.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Represents a College's login account on the counselling dashboard.
 * One college (identified by collegeCode) gets one account.
 * Linked to the colleges table via collegeCode (not a FK — keeps it loose).
 *
 * TABLE: college_accounts
 *
 * HOW IT WORKS:
 *  - Admin creates a college account (via admin panel or seeder)
 *    OR college self-registers with their DTE college code.
 *  - Login uses email + password.
 *  - JWT token is issued with role = COLLEGE and subject = collegeCode.
 *  - Dashboard API reads collegeCode from JWT, so college only sees their own data.
 */
@Entity
@Table(name = "college_accounts", indexes = {
    @Index(name = "idx_ca_college_code", columnList = "college_code", unique = true),
    @Index(name = "idx_ca_email",         columnList = "email",         unique = true)
})
public class CollegeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** DTE college code — e.g. "06155". Links to colleges.college_code. */
    @Column(name = "college_code", nullable = false, unique = true, length = 10)
    private String collegeCode;

    /** Login email — typically the college's official email. */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt-hashed password. */
    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    /** Contact person's name at the college (HOD / Principal / Admission Head). */
    @Column(name = "contact_person_name", length = 100)
    private String contactPersonName;

    /** Contact person's phone number. */
    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    /**
     * Account status.
     * false = pending admin approval (default after self-registration)
     * true  = active, can log in
     *
     * Admin-created accounts should be set to active immediately.
     */
    @Column(name = "is_approved", nullable = false)
    private boolean approved = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                              { return id; }

    public String getCollegeCode()                   { return collegeCode; }
    public void setCollegeCode(String v)             { this.collegeCode = v; }

    public String getEmail()                         { return email; }
    public void setEmail(String v)                   { this.email = v; }

    public String getPasswordHash()                  { return passwordHash; }
    public void setPasswordHash(String v)            { this.passwordHash = v; }

    public String getContactPersonName()             { return contactPersonName; }
    public void setContactPersonName(String v)       { this.contactPersonName = v; }

    public String getContactPhone()                  { return contactPhone; }
    public void setContactPhone(String v)            { this.contactPhone = v; }

    public boolean isApproved()                      { return approved; }
    public void setApproved(boolean v)               { this.approved = v; }

    public boolean isActive()                        { return active; }
    public void setActive(boolean v)                 { this.active = v; }

    public LocalDateTime getCreatedAt()              { return createdAt; }

    public LocalDateTime getLastLoginAt()            { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v)      { this.lastLoginAt = v; }
}