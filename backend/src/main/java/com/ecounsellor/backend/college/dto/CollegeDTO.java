package com.ecounsellor.backend.college.dto;

public class CollegeDTO {
    
    private Long collegeId;
    private String collegeCode;
    private String collegeName;
    private String courseUniversity;

    // Default Constructor
    public CollegeDTO() {}

    // Constructor with all fields
    public CollegeDTO(Long collegeId, String collegeCode, String collegeName, String courseUniversity) {
        this.collegeId = collegeId;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.courseUniversity = courseUniversity;
    }

    // Getters and Setters
    public Long getCollegeId() {
        return collegeId;
    }

    public void setCollegeId(Long collegeId) {
        this.collegeId = collegeId;
    }

    public String getCollegeCode() {
        return collegeCode;
    }

    public void setCollegeCode(String collegeCode) {
        this.collegeCode = collegeCode;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getCourseUniversity() {
        return courseUniversity;
    }

    public void setCourseUniversity(String courseUniversity) {
        this.courseUniversity = courseUniversity;
    }
}
