package com.ecounsellor.backend.counselling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks when a student views a college detail page.
 * NO personal info stored — only percentile, category, gender.
 * Privacy-safe by design: colleges see only aggregated counts, never individual rows.
 */
@Entity
@Table(name = "student_views", indexes = {
    @Index(name = "idx_sv_college_code", columnList = "college_code"),
    @Index(name = "idx_sv_viewed_at",    columnList = "viewed_at")
})
public class StudentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_code", nullable = false, length = 10)
    private String collegeCode;

    @Column(name = "course_code", length = 20)
    private String courseCode;          // which branch was viewed (nullable = college-level view)

    @Column(name = "student_percentile")
    private Double studentPercentile;   // anonymous — no name/phone

    @Column(name = "category", length = 10)
    private String category;            // OPEN, OBC, SC, ST, NT1 ...

    @Column(name = "gender", length = 10)
    private String gender;              // GENERAL, LADIES

    @Column(name = "admission_type", length = 10)
    private String admissionType;       // STATE, HOME, OTHER

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public String getCollegeCode()               { return collegeCode; }
    public void setCollegeCode(String v)         { this.collegeCode = v; }
    public String getCourseCode()                { return courseCode; }
    public void setCourseCode(String v)          { this.courseCode = v; }
    public Double getStudentPercentile()         { return studentPercentile; }
    public void setStudentPercentile(Double v)   { this.studentPercentile = v; }
    public String getCategory()                  { return category; }
    public void setCategory(String v)            { this.category = v; }
    public String getGender()                    { return gender; }
    public void setGender(String v)              { this.gender = v; }
    public String getAdmissionType()             { return admissionType; }
    public void setAdmissionType(String v)       { this.admissionType = v; }
    public LocalDateTime getViewedAt()           { return viewedAt; }
    public void setViewedAt(LocalDateTime v)     { this.viewedAt = v; }
}
