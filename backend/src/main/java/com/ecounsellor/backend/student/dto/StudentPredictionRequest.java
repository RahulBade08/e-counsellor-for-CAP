package com.ecounsellor.backend.student.dto;

import java.util.List;
import java.util.stream.Collectors;

public class StudentPredictionRequest {

    private Double  percentile;
    private String  category;
    private String  gender;
    private Integer round;
    private String  admissionType = "STATE";
    private List<String> branches;
    private List<String> districts;

    public Double  getPercentile()             { return percentile; }
    public void    setPercentile(Double p)     { this.percentile = p; }

    public String  getCategory()               { return category; }
    public void    setCategory(String c)       { this.category = c; }

    public String  getGender()                 { return gender; }
    public void    setGender(String g)         { this.gender = g; }

    public Integer getRound()                  { return round; }
    public void    setRound(Integer r)         { this.round = r; }

    public String  getAdmissionType()                     { return admissionType; }
    public void    setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public List<String> getBranches()               { return branches; }
    public void         setBranches(List<String> b) { this.branches = b; }
    public void         setBranch(List<String> b)   { this.branches = b; }

    public List<String> getDistricts()               { return districts; }
    public void         setDistricts(List<String> d) { this.districts = d; }
    public void         setDistrict(List<String> d)  { this.districts = d; }

    public boolean hasBranchFilter()   { return branches  != null && !branches.isEmpty(); }
    public boolean hasDistrictFilter() { return districts != null && !districts.isEmpty(); }

    /**
     * Expands group labels to exact DB course names via BranchGroups.
     * "Computer Science" -> ["Computer Engineering", "Computer Science and Engineering", ...]
     * "Computer Science and Engineering" (exact) -> passes through unchanged.
     */
    public List<String> expandedBranches() {
        return BranchGroups.expand(branches);
    }

    /**
     * Districts lowercased to match LOWER(col.district) in SQL query.
     * Handles any case inconsistency in DB rows.
     */
    public List<String> districtListLower() {
        if (districts == null || districts.isEmpty()) return List.of();
        return districts.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    /**
     * Derives the cap_category_code from category + gender + admissionType.
     * OPEN + GENERAL + HOME  -> GOPENH
     * OBC  + GENERAL + STATE -> GOBCS
     * SC   + LADIES  + OTHER -> LSCO
     * EWS  -> EWS (flat, no prefix/suffix)
     * TFWS -> TFWS (flat, no prefix/suffix)
     */
    public String derivedCapCategoryCode() {
        if (category != null) {
            String cat = category.toUpperCase().trim();
            if (cat.equals("EWS"))  return "EWS";
            if (cat.equals("TFWS")) return "TFWS";
        }
        String prefix = (gender != null && gender.equalsIgnoreCase("LADIES")) ? "L" : "G";
        String suffix = admissionTypeSuffix();
        String mid    = normalizeCategory(category);
        return prefix + mid + suffix;
    }

    private String admissionTypeSuffix() {
        if (admissionType == null) return "S";
        return switch (admissionType.toUpperCase().trim()) {
            case "HOME"  -> "H";
            case "OTHER" -> "O";
            default      -> "S";
        };
    }

    private String normalizeCategory(String cat) {
        if (cat == null) return "OPEN";
        return switch (cat.toUpperCase().trim()) {
            case "OPEN"              -> "OPEN";
            case "OBC"               -> "OBC";
            case "SC"                -> "SC";
            case "ST"                -> "ST";
            case "NT-1", "NT1", "VJ" -> "NT1";
            case "NT-2", "NT2"       -> "NT2";
            case "NT-3", "NT3"       -> "NT3";
            default                  -> cat.toUpperCase().trim();
        };
    }
}