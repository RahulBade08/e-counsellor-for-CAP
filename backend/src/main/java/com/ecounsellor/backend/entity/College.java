package com.ecounsellor.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "colleges")
@Data
public class College {

    @Id
    private String collegeCode;
    private String collegeName;
    private String district;
    private String type;
    private Boolean autonomous;
    private String naacGrade;
    private Integer nirfRank;
    private Double avgPackageLpa;

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
	public String getDistrict() {
		return district;
	}
	public void setDistrict(String district) {
		this.district = district;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean getAutonomous() {
		return autonomous;
	}
	public void setAutonomous(Boolean autonomous) {
		this.autonomous = autonomous;
	}
	public String getNaacGrade() {
		return naacGrade;
	}
	public void setNaacGrade(String naacGrade) {
		this.naacGrade = naacGrade;
	}
	public Integer getNirfRank() {
		return nirfRank;
	}
	public void setNirfRank(Integer nirfRank) {
		this.nirfRank = nirfRank;
	}
	public Double getAvgPackageLpa() {
		return avgPackageLpa;
	}
	public void setAvgPackageLpa(Double avgPackageLpa) {
		this.avgPackageLpa = avgPackageLpa;
	}
	
}
