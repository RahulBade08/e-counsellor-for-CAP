package com.ecounsellor.backend.college.dto;

import java.util.List;

public class CollegeDTO {

    private Long collegeId;
    private String collegeCode;
    private String collegeName;
    private String courseUniversity;

    // ── New fields ───────────────────────────────────────────────────────────
    private String fundingType;
    private Boolean isAutonomous;
    private String minorityStatus;
    private Integer totalIntake;
    private String address;
    private String region;
    private String district;

    // ── Courses list (full detail per college) ───────────────────────────────
    private List<CourseDTO> courses;

    // ── Nested CourseDTO ─────────────────────────────────────────────────────
    public static class CourseDTO {
        private Long courseId;
        private String courseCode;
        private String courseName;
        private String courseStatus;
        private Integer intake;
        private String university;
        private Boolean isAutonomous;
        private String minorityStatus;
        private String shift;
        private String accreditation;
        private String gender;

        public CourseDTO() {}

        public CourseDTO(Long courseId, String courseCode, String courseName,
                         String courseStatus, Integer intake, String university,
                         Boolean isAutonomous, String minorityStatus,
                         String shift, String accreditation, String gender) {
            this.courseId = courseId;
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseStatus = courseStatus;
            this.intake = intake;
            this.university = university;
            this.isAutonomous = isAutonomous;
            this.minorityStatus = minorityStatus;
            this.shift = shift;
            this.accreditation = accreditation;
            this.gender = gender;
        }

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }

        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public String getCourseStatus() { return courseStatus; }
        public void setCourseStatus(String courseStatus) { this.courseStatus = courseStatus; }

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

    // ── CollegeDTO constructors ───────────────────────────────────────────────
    public CollegeDTO() {}

    public CollegeDTO(Long collegeId, String collegeCode, String collegeName,
                      String courseUniversity, String fundingType, Boolean isAutonomous,
                      String minorityStatus, Integer totalIntake, String address,
                      String region, String district, List<CourseDTO> courses) {
        this.collegeId = collegeId;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.courseUniversity = courseUniversity;
        this.fundingType = fundingType;
        this.isAutonomous = isAutonomous;
        this.minorityStatus = minorityStatus;
        this.totalIntake = totalIntake;
        this.address = address;
        this.region = region;
        this.district = district;
        this.courses = courses;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────
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

    public List<CourseDTO> getCourses() { return courses; }
    public void setCourses(List<CourseDTO> courses) { this.courses = courses; }
}