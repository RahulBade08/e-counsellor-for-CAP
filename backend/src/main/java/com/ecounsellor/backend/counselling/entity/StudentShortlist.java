package com.ecounsellor.backend.counselling.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks when a student explicitly shortlists a college+branch.
 * Stronger signal than a view — student tapped "Shortlist" in the app.
 * Still privacy-safe: no name, phone, or app account needed to record this.
 */
@Entity
@Table(name = "student_shortlists", indexes = {
    @Index(name = "idx_ss_college_code",  columnList = "college_code"),
    @Index(name = "idx_ss_course_code",   columnList = "course_code"),
    @Index(name = "idx_ss_shortlisted_at",columnList = "shortlisted_at")
})
public class StudentShortlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "college_code", nullable = false, length = 10)
    private String collegeCode;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;          // branch shortlisted

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "student_percentile")
    private Double studentPercentile;

    @Column(name = "category", length = 10)
    private String category;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "admission_type", length = 10)
    private String admissionType;

    @Column(name = "cap_category_code", length = 15)
    private String capCategoryCode;     // derived code e.g. GOPENH, GOBCS

    @Column(name = "shortlisted_at", nullable = false)
    private LocalDateTime shortlistedAt;

    @PrePersist
    protected void onCreate() {
        if (shortlistedAt == null) shortlistedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public String getCollegeCode()               { return collegeCode; }
    public void setCollegeCode(String v)         { this.collegeCode = v; }
    public String getCourseCode()                { return courseCode; }
    public void setCourseCode(String v)          { this.courseCode = v; }
    public String getCourseName()                { return courseName; }
    public void setCourseName(String v)          { this.courseName = v; }
    public Double getStudentPercentile()         { return studentPercentile; }
    public void setStudentPercentile(Double v)   { this.studentPercentile = v; }
    public String getCategory()                  { return category; }
    public void setCategory(String v)            { this.category = v; }
    public String getGender()                    { return gender; }
    public void setGender(String v)              { this.gender = v; }
    public String getAdmissionType()             { return admissionType; }
    public void setAdmissionType(String v)       { this.admissionType = v; }
    public String getCapCategoryCode()           { return capCategoryCode; }
    public void setCapCategoryCode(String v)     { this.capCategoryCode = v; }
    public LocalDateTime getShortlistedAt()      { return shortlistedAt; }
    public void setShortlistedAt(LocalDateTime v){ this.shortlistedAt = v; }
}
