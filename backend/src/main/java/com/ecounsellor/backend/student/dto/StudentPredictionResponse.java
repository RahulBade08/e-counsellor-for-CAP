package com.ecounsellor.backend.student.dto;

public class StudentPredictionResponse {

    private String collegeName;
    private String courseName;
    private Double cutoffPercentile;
    private Integer round;
    private String risk;
    private double probability;
    private String confidence;


    public StudentPredictionResponse(
            String collegeName,
            String courseName,
            Double cutoffPercentile,
            Integer round,
            String risk,
            double probability,
            String confidence
    ) {
        this.collegeName = collegeName;
        this.courseName = courseName;
        this.cutoffPercentile = cutoffPercentile;
        this.round = round;
        this.risk = risk;
        this.probability = probability;
        this.confidence = confidence;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public String getCourseName() {
        return courseName;
    }

    public Double getCutoffPercentile() {
        return cutoffPercentile;
    }

    public Integer getRound() {
        return round;
    }

    public String getRisk() {
        return risk;
    }

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public String getConfidence() {
		return confidence;
	}

	public void setConfidence(String confidence) {
		this.confidence = confidence;
	}
    
    
}
