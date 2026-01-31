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
