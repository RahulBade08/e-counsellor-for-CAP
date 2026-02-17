package com.ecounsellor.backend.student.dto;

public class StudentPredictionResponse {

    private String collegeName;
    private String collegeCode;
    private String courseName;
    private Double cutoffPercentile;
    private Integer round;
    private String risk;
    private double probability;
    private String confidence;

    // ── New location fields ───────────────────────────────────────────────────
    private String district;
    private String region;
    private String address;
    private String fundingType;
    private Boolean isAutonomous;
    private Integer intake;

    public StudentPredictionResponse(
            String collegeName,
            String collegeCode,
            String courseName,
            Double cutoffPercentile,
            Integer round,
            String risk,
            double probability,
            String confidence,
            String district,
            String region,
            String address,
            String fundingType,
            Boolean isAutonomous,
            Integer intake
    ) {
        this.collegeName       = collegeName;
        this.collegeCode       = collegeCode;
        this.courseName        = courseName;
        this.cutoffPercentile  = cutoffPercentile;
        this.round             = round;
        this.risk              = risk;
        this.probability       = probability;
        this.confidence        = confidence;
        this.district          = district;
        this.region            = region;
        this.address           = address;
        this.fundingType       = fundingType;
        this.isAutonomous      = isAutonomous;
        this.intake            = intake;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getCollegeName()      { return collegeName; }
    public String getCollegeCode()      { return collegeCode; }
    public String getCourseName()       { return courseName; }
    public Double getCutoffPercentile() { return cutoffPercentile; }
    public Integer getRound()           { return round; }
    public String getRisk()             { return risk; }
    public double getProbability()      { return probability; }
    public String getConfidence()       { return confidence; }
    public String getDistrict()         { return district; }
    public String getRegion()           { return region; }
    public String getAddress()          { return address; }
    public String getFundingType()      { return fundingType; }
    public Boolean getIsAutonomous()    { return isAutonomous; }
    public Integer getIntake()          { return intake; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setCollegeName(String collegeName)           { this.collegeName = collegeName; }
    public void setCollegeCode(String collegeCode)           { this.collegeCode = collegeCode; }
    public void setCourseName(String courseName)             { this.courseName = courseName; }
    public void setCutoffPercentile(Double cutoffPercentile) { this.cutoffPercentile = cutoffPercentile; }
    public void setRound(Integer round)                      { this.round = round; }
    public void setRisk(String risk)                         { this.risk = risk; }
    public void setProbability(double probability)           { this.probability = probability; }
    public void setConfidence(String confidence)             { this.confidence = confidence; }
    public void setDistrict(String district)                 { this.district = district; }
    public void setRegion(String region)                     { this.region = region; }
    public void setAddress(String address)                   { this.address = address; }
    public void setFundingType(String fundingType)           { this.fundingType = fundingType; }
    public void setIsAutonomous(Boolean isAutonomous)        { this.isAutonomous = isAutonomous; }
    public void setIntake(Integer intake)                    { this.intake = intake; }
}