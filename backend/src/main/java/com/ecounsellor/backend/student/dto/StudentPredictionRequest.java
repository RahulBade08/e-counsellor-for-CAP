package com.ecounsellor.backend.student.dto;

import java.util.List;

public class StudentPredictionRequest {

    private Double  percentile;
    private String  category;
    private String  gender;
    private Integer round;

    /**
     * Admission type — student selects one of:
     *   "STATE"  → State Level seats          (suffix S, e.g. GOBCS)
     *   "HOME"   → Home University seats      (suffix H, e.g. GOBCH)
     *   "OTHER"  → Other University seats     (suffix O, e.g. GOBCO)
     *
     * Default: "STATE" (most common for Maharashtra students)
     */
    private String admissionType = "STATE";

    // Multi-select filters (empty = no filter)
    private List<String> branches;
    private List<String> districts;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Double  getPercentile()             { return percentile; }
    public void    setPercentile(Double p)     { this.percentile = p; }

    public String  getCategory()               { return category; }
    public void    setCategory(String c)       { this.category = c; }

    public String  getGender()                 { return gender; }
    public void    setGender(String g)         { this.gender = g; }

    public Integer getRound()                  { return round; }
    public void    setRound(Integer r)         { this.round = r; }

    public String  getAdmissionType()                   { return admissionType; }
    public void    setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public List<String> getBranches()               { return branches; }
    public void         setBranches(List<String> b) { this.branches = b; }
    public void         setBranch(List<String> b)   { this.branches = b; } // alias

    public List<String> getDistricts()               { return districts; }
    public void         setDistricts(List<String> d) { this.districts = d; }
    public void         setDistrict(List<String> d)  { this.districts = d; } // alias

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hasBranchFilter()   { return branches  != null && !branches.isEmpty(); }
    public boolean hasDistrictFilter() { return districts != null && !districts.isEmpty(); }

    public List<String> getBranchesLower() {
        if (branches == null || branches.isEmpty()) return List.of();
        return branches.stream().map(String::toLowerCase).toList();
    }

    public List<String> getDistrictsLower() {
        if (districts == null || districts.isEmpty()) return List.of();
        return districts.stream().map(String::toLowerCase).toList();
    }

    /**
     * Derives the exact cap_category_code stored in DB.
     *
     * Structure:  [gender prefix] + [category] + [admission suffix]
     *
     * Gender prefix:
     *   G → GENERAL (male or co-ed)
     *   L → LADIES
     *
     * Category middle:
     *   OPEN, OBC, SC, ST, NT1, NT2, NT3, EWS, TFWS
     *
     * Admission suffix:
     *   S → State Level
     *   H → Home University
     *   O → Other University
     *
     * Special cases (no prefix/suffix):
     *   EWS  → always "EWS"
     *   TFWS → always "TFWS"
     *   DEF* → Defence quota (not handled here)
     *   PWD* → PwD quota    (not handled here)
     *
     * Examples:
     *   OBC  + GENERAL + STATE  → GOBCS
     *   OBC  + GENERAL + HOME   → GOBCH
     *   OBC  + GENERAL + OTHER  → GOBCO
     *   OPEN + LADIES  + HOME   → LOPENH
     *   SC   + GENERAL + STATE  → GSCS
     *   NT-1 + LADIES  + OTHER  → LNT1O
     */
    public String derivedCapCategoryCode() {
        // Special flat codes — no prefix/suffix
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
            default      -> "S"; // STATE is default
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