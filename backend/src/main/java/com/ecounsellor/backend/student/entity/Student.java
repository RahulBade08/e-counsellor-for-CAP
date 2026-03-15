package com.ecounsellor.backend.student.entity;

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

@Entity
@Table(name = "students", indexes = {
    @Index(name = "idx_student_phone",   columnList = "phone",          unique = true),
    @Index(name = "idx_student_cet_app", columnList = "cet_app_number")
})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, unique = true, length = 15)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "cet_app_number", length = 30)
    private String cetAppNumber;

    @Column(name = "cet_percentile")
    private Double cetPercentile;

    @Column(name = "category", length = 10)
    private String category;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "admission_type", length = 10)
    private String admissionType;

    @Column(name = "preferred_branches", columnDefinition = "TEXT")
    private String preferredBranches;

    @Column(name = "preferred_districts", columnDefinition = "TEXT")
    private String preferredDistricts;

    // Stores shortlisted colleges as a JSON array of objects:
    // [{"collegeCode":"06298","courseName":"Computer Engineering","collegeName":"..."},...]
    @Column(name = "shortlisted_colleges", columnDefinition = "TEXT")
    private String shortlistedColleges;

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

    public Long getId()                              { return id; }
    public String getPhone()                         { return phone; }
    public void setPhone(String v)                   { this.phone = v; }
    public String getPasswordHash()                  { return passwordHash; }
    public void setPasswordHash(String v)            { this.passwordHash = v; }
    public String getName()                          { return name; }
    public void setName(String v)                    { this.name = v; }
    public String getCetAppNumber()                  { return cetAppNumber; }
    public void setCetAppNumber(String v)            { this.cetAppNumber = v; }
    public Double getCetPercentile()                 { return cetPercentile; }
    public void setCetPercentile(Double v)           { this.cetPercentile = v; }
    public String getCategory()                      { return category; }
    public void setCategory(String v)                { this.category = v; }
    public String getGender()                        { return gender; }
    public void setGender(String v)                  { this.gender = v; }
    public String getAdmissionType()                 { return admissionType; }
    public void setAdmissionType(String v)           { this.admissionType = v; }
    public String getPreferredBranches()             { return preferredBranches; }
    public void setPreferredBranches(String v)       { this.preferredBranches = v; }
    public String getPreferredDistricts()            { return preferredDistricts; }
    public void setPreferredDistricts(String v)      { this.preferredDistricts = v; }
    public String getShortlistedColleges()           { return shortlistedColleges; }
    public void setShortlistedColleges(String v)     { this.shortlistedColleges = v; }
    public boolean isActive()                        { return active; }
    public void setActive(boolean v)                 { this.active = v; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public LocalDateTime getLastLoginAt()            { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v)      { this.lastLoginAt = v; }
}