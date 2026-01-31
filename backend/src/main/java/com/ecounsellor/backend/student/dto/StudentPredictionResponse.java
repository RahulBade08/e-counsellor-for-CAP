package com.ecounsellor.backend.student.dto;

public class StudentPredictionResponse {

    private String collegeName;
    private String courseName;
    private Double cutoffPercentile;
    private Integer round;
    private String risk;

    public StudentPredictionResponse(
            String collegeName,
            String courseName,
            Double cutoffPercentile,
            Integer round,
            String risk
    ) {
        this.collegeName = collegeName;
        this.courseName = courseName;
        this.cutoffPercentile = cutoffPercentile;
        this.round = round;
        this.risk = risk;
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
}
