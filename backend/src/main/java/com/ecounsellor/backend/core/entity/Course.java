package com.ecounsellor.backend.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "courses",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"course_code", "college_id"})
    }
)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "course_status")
    private String courseStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    // ── New fields from MHT-CET scrape ──────────────────────────────────────

    @Column(name = "intake")
    private Integer intake;                 // Sanctioned intake (excluding TFW)

    @Column(name = "university")
    private String university;             // Affiliating university

    @Column(name = "is_autonomous")
    private Boolean isAutonomous;

    @Column(name = "minority_status")
    private String minorityStatus;

    @Column(name = "shift")
    private String shift;                  // General Shift / Morning Shift

    @Column(name = "accreditation")
    private String accreditation;          // NBA etc.

    @Column(name = "gender")
    private String gender;                 // Co-Education / Female

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseStatus() { return courseStatus; }
    public void setCourseStatus(String courseStatus) { this.courseStatus = courseStatus; }

    public College getCollege() { return college; }
    public void setCollege(College college) { this.college = college; }

    public Integer getIntake() { return intake; }
    public void setIntake(Integer intake) { this.intake = intake; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public Boolean getIsAutonomous() { return isAutonomous; }
    public void setIsAutonomous(Boolean isAutonomous) { this.isAutonomous = isAutonomous; }

    public String getMinorityStatus() { return minorityStatus; }
    public void setMinorityStatus(String minorityStatus) { this.minorityStatus = minorityStatus; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public String getAccreditation() { return accreditation; }
    public void setAccreditation(String accreditation) { this.accreditation = accreditation; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
}