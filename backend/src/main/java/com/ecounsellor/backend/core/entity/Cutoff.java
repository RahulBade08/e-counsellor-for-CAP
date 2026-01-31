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

@Entity
@Table(name = "cutoffs")

public class Cutoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cutoffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "cap_category_code")
    private String capCategoryCode;

    @Column(name = "gender")
    private String gender;

    @Column(name = "regional_reservation")
    private String regionalReservation;

    @Column(name = "round")
    private Integer round;

    @Column(name = "last_cap_round")
    private Integer lastCapRound;

    @Column(name = "last_rank")
    private Integer lastRank;

    @Column(name = "cutoff_percentile")
    private Double cutoffPercentile;

	public Long getCutoffId() {
		return cutoffId;
	}

	public void setCutoffId(Long cutoffId) {
		this.cutoffId = cutoffId;
	}

	public Course getCourse() {
		return course;
	}

	public void setCourse(Course course) {
		this.course = course;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getCapCategoryCode() {
		return capCategoryCode;
	}

	public void setCapCategoryCode(String capCategoryCode) {
		this.capCategoryCode = capCategoryCode;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getRegionalReservation() {
		return regionalReservation;
	}

	public void setRegionalReservation(String regionalReservation) {
		this.regionalReservation = regionalReservation;
	}

	public Integer getRound() {
		return round;
	}

	public void setRound(Integer round) {
		this.round = round;
	}

	public Integer getLastCapRound() {
		return lastCapRound;
	}

	public void setLastCapRound(Integer lastCapRound) {
		this.lastCapRound = lastCapRound;
	}

	public Integer getLastRank() {
		return lastRank;
	}

	public void setLastRank(Integer lastRank) {
		this.lastRank = lastRank;
	}

	public Double getCutoffPercentile() {
		return cutoffPercentile;
	}

	public void setCutoffPercentile(Double cutoffPercentile) {
		this.cutoffPercentile = cutoffPercentile;
	}

    
}
