package com.ecounsellor.backend.student.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A registered student account.
 *
 * How student proves identity:
 *   - Phone number (unique, used as username)
 *   - Password (BCrypt hashed — student sets this on registration)
 *   - CET Application Number (self-reported, stored for reference)
 *   - Percentile (self-reported from their result card)
 *
 * We cannot verify CET score from DTE — student types it themselves.
 * The phone number is the identity anchor (unique per account).
 */
@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_student_phone",   columnList = "phone",            unique = true),
    @Index(name = "idx_student_cet_app", columnList = "cet_app_number")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity ──────────────────────────────────────────────────────────────
    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;                   // +91XXXXXXXXXX — login username

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;            // BCrypt

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // ── CET Data (self-reported) ───────────────────────────────────────────────
    @Column(name = "cet_app_number", length = 30)
    private String cetAppNumber;            // from hall ticket e.g. 24-CET-012345

    @Column(name = "cet_percentile")
    private Double cetPercentile;           // e.g. 82.4

    // ── Profile (saved preferences — pre-fill form on next login) ─────────────
    @Column(name = "category", length = 10)
    private String category;               // OPEN, OBC, SC, ST, NT1, NT2, NT3, EWS, TFWS

    @Column(name = "gender", length = 10)
    private String gender;                 // GENERAL, LADIES

    @Column(name = "admission_type", length = 10)
    private String admissionType;          // STATE, HOME, OTHER

    @Column(name = "preferred_branches", columnDefinition = "TEXT")
    private String preferredBranches;      // JSON array stored as text e.g. ["Computer Science","IT"]

    @Column(name = "preferred_districts", columnDefinition = "TEXT")
    private String preferredDistricts;     // JSON array e.g. ["Pune","Nashik"]

    // ── Account status ────────────────────────────────────────────────────────
    @Column(name = "is_active")
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

    public Long getId()                            { return id; }
    public String getPhone()                       { return phone; }
    public void setPhone(String v)                 { this.phone = v; }
    public String getPasswordHash()                { return passwordHash; }
    public void setPasswordHash(String v)          { this.passwordHash = v; }
    public String getName()                        { return name; }
    public void setName(String v)                  { this.name = v; }
    public String getCetAppNumber()                { return cetAppNumber; }
    public void setCetAppNumber(String v)          { this.cetAppNumber = v; }
    public Double getCetPercentile()               { return cetPercentile; }
    public void setCetPercentile(Double v)         { this.cetPercentile = v; }
    public String getCategory()                    { return category; }
    public void setCategory(String v)              { this.category = v; }
    public String getGender()                      { return gender; }
    public void setGender(String v)                { this.gender = v; }
    public String getAdmissionType()               { return admissionType; }
    public void setAdmissionType(String v)         { this.admissionType = v; }
    public String getPreferredBranches()           { return preferredBranches; }
    public void setPreferredBranches(String v)     { this.preferredBranches = v; }
    public String getPreferredDistricts()          { return preferredDistricts; }
    public void setPreferredDistricts(String v)    { this.preferredDistricts = v; }
    public boolean isActive()                      { return active; }
    public void setActive(boolean v)               { this.active = v; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public LocalDateTime getLastLoginAt()          { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v)    { this.lastLoginAt = v; }
}
