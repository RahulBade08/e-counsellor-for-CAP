package com.ecounsellor.backend.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "colleges")
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collegeId;

    @Column(name = "college_code", nullable = false, unique = true)
    private String collegeCode;

    @Column(name = "college_name", nullable = false)
    private String collegeName;

    @Column(name = "course_university")
    private String courseUniversity;

    // ── New fields from MHT-CET scrape ──────────────────────────────────────

    @Column(name = "funding_type")
    private String fundingType;             // Government / Un-Aided / Government-Aided / University

    @Column(name = "is_autonomous")
    private Boolean isAutonomous;

    @Column(name = "minority_status")
    private String minorityStatus;          // Non-Minority / Linguistic Minority - Hindi / etc.

    @Column(name = "total_intake")
    private Integer totalIntake;

    @Column(name = "address")
    private String address;

    @Column(name = "region")
    private String region;                  // Amravati / Aurangabad / Mumbai / Nagpur / Nashik / Pune

    @Column(name = "district")
    private String district;               // Pune / Thane / Nagpur / etc.

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getCollegeId() { return collegeId; }
    public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }

    public String getCollegeCode() { return collegeCode; }
    public void setCollegeCode(String collegeCode) { this.collegeCode = collegeCode; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getCourseUniversity() { return courseUniversity; }
    public void setCourseUniversity(String courseUniversity) { this.courseUniversity = courseUniversity; }

    public String getFundingType() { return fundingType; }
    public void setFundingType(String fundingType) { this.fundingType = fundingType; }

    public Boolean getIsAutonomous() { return isAutonomous; }
    public void setIsAutonomous(Boolean isAutonomous) { this.isAutonomous = isAutonomous; }

    public String getMinorityStatus() { return minorityStatus; }
    public void setMinorityStatus(String minorityStatus) { this.minorityStatus = minorityStatus; }

    public Integer getTotalIntake() { return totalIntake; }
    public void setTotalIntake(Integer totalIntake) { this.totalIntake = totalIntake; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
}