package com.ecounsellor.backend.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Single row sent by the Admin Panel Data Import wizard (POST /api/admin/import/push).
 *
 * Supports two import sources:
 *   1. extractor.py CSV output  — uses category_reservation, regional_reservation,
 *                                  last_cap_round, course_status, course_university
 *   2. Generic upload CSV       — uses cap_category
 *
 * The service layer merges these: category_reservation takes priority over cap_category.
 *
 * NOTE: gender field removed — not used in cutoff schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportRow {

    // ── Core fields (both sources) ────────────────────────────────────────────

    private String college_code;
    private String college_name;
    private String course_code;
    private String course_name;
    private Integer last_rank;
    private Double  cutoff_percentile;

    // ── Generic upload CSV field name ─────────────────────────────────────────
    private String cap_category;

    // ── extractor.py specific field names ────────────────────────────────────

    /** extractor.py column: category_reservation (e.g. GOPENH, LOBCH, SC, etc.) */
    private String  category_reservation;

    /** extractor.py column: regional_reservation
     *  e.g. "Home University Seats Allotted to Home University Candidates" */
    private String  regional_reservation;

    /** extractor.py column: last_cap_round (1 = CAP Round I, 2 = Round II, etc.) */
    private Integer last_cap_round;

    /** extractor.py column: course_status (Open / Closed) */
    private String  course_status;

    /** extractor.py column: course_university (affiliating university) */
    private String  course_university;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getCollege_code()                       { return college_code; }
    public void   setCollege_code(String v)               { this.college_code = v; }

    public String getCollege_name()                       { return college_name; }
    public void   setCollege_name(String v)               { this.college_name = v; }

    public String getCourse_code()                        { return course_code; }
    public void   setCourse_code(String v)                { this.course_code = v; }

    public String getCourse_name()                        { return course_name; }
    public void   setCourse_name(String v)                { this.course_name = v; }

    public Integer getLast_rank()                         { return last_rank; }
    public void    setLast_rank(Integer v)                { this.last_rank = v; }

    public Double  getCutoff_percentile()                 { return cutoff_percentile; }
    public void    setCutoff_percentile(Double v)         { this.cutoff_percentile = v; }

    public String getCap_category()                       { return cap_category; }
    public void   setCap_category(String v)               { this.cap_category = v; }

    public String getCategory_reservation()               { return category_reservation; }
    public void   setCategory_reservation(String v)       { this.category_reservation = v; }

    public String getRegional_reservation()               { return regional_reservation; }
    public void   setRegional_reservation(String v)       { this.regional_reservation = v; }

    public Integer getLast_cap_round()                    { return last_cap_round; }
    public void    setLast_cap_round(Integer v)           { this.last_cap_round = v; }

    public String getCourse_status()                      { return course_status; }
    public void   setCourse_status(String v)              { this.course_status = v; }

    public String getCourse_university()                  { return course_university; }
    public void   setCourse_university(String v)          { this.course_university = v; }

    /**
     * Resolved category: prefers category_reservation (extractor.py output),
     * falls back to cap_category (manual upload).
     * Service layer should call this instead of accessing fields directly.
     */
    public String resolvedCategory() {
        if (category_reservation != null && !category_reservation.isBlank())
            return category_reservation.toUpperCase().strip();
        if (cap_category != null && !cap_category.isBlank())
            return cap_category.toUpperCase().strip();
        return "UNKNOWN";
    }

    /**
     * Safety check: ensure cutoff_percentile is always stored as a positive value.
     * Handles the extractor.py bug where negatives appear in the raw CSV.
     */
    public Double getSafeCutoffPercentile() {
        if (cutoff_percentile == null) return null;
        return Math.abs(cutoff_percentile);
    }
}