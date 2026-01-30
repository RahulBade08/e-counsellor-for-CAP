package com.ecounsellor.backend.college.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "seat_matrix")
@Data
public class SeatMatrix {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String collegeCode;
    private String branch;
    private Integer totalSeats;
    private Integer openSeats;
    private Integer obcSeats;
    private Integer scSeats;
    private Integer stSeats;
    private Integer ewsSeats;
    private String year;

    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCollegeCode() {
		return collegeCode;
	}
	public void setCollegeCode(String collegeCode) {
		this.collegeCode = collegeCode;
	}
	public String getBranch() {
		return branch;
	}
	public void setBranch(String branch) {
		this.branch = branch;
	}
	public Integer getTotalSeats() {
		return totalSeats;
	}
	public void setTotalSeats(Integer totalSeats) {
		this.totalSeats = totalSeats;
	}
	public Integer getOpenSeats() {
		return openSeats;
	}
	public void setOpenSeats(Integer openSeats) {
		this.openSeats = openSeats;
	}
	public Integer getObcSeats() {
		return obcSeats;
	}
	public void setObcSeats(Integer obcSeats) {
		this.obcSeats = obcSeats;
	}
	public Integer getScSeats() {
		return scSeats;
	}
	public void setScSeats(Integer scSeats) {
		this.scSeats = scSeats;
	}
	public Integer getStSeats() {
		return stSeats;
	}
	public void setStSeats(Integer stSeats) {
		this.stSeats = stSeats;
	}
	public Integer getEwsSeats() {
		return ewsSeats;
	}
	public void setEwsSeats(Integer ewsSeats) {
		this.ewsSeats = ewsSeats;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	
}
