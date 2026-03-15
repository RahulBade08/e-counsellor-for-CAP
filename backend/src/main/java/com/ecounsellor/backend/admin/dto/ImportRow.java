package com.ecounsellor.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Single row coming from the frontend after CSV parse + data cleaning.
 * Maps directly to what the Data Import page sends in POST /api/admin/import/push.
 *
 * Field names use snake_case to match what the React frontend sends.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportRow {

    private String college_code;
    private String college_name;
    private String course_code;
    private String course_name;
    private String cap_category;
    private String gender;
    private Integer round;
    private Integer last_rank;
    private Double cutoff_percentile;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getCollege_code()                     { return college_code; }
    public void setCollege_code(String college_code)   { this.college_code = college_code; }

    public String getCollege_name()                     { return college_name; }
    public void setCollege_name(String college_name)   { this.college_name = college_name; }

    public String getCourse_code()                      { return course_code; }
    public void setCourse_code(String course_code)     { this.course_code = course_code; }

    public String getCourse_name()                      { return course_name; }
    public void setCourse_name(String course_name)     { this.course_name = course_name; }

    public String getCap_category()                     { return cap_category; }
    public void setCap_category(String cap_category)   { this.cap_category = cap_category; }

    public String getGender()                           { return gender; }
    public void setGender(String gender)               { this.gender = gender; }

    public Integer getRound()                           { return round; }
    public void setRound(Integer round)               { this.round = round; }

    public Integer getLast_rank()                       { return last_rank; }
    public void setLast_rank(Integer last_rank)       { this.last_rank = last_rank; }

    public Double getCutoff_percentile()                { return cutoff_percentile; }
    public void setCutoff_percentile(Double v)        { this.cutoff_percentile = v; }
}
