package com.ecounsellor.backend.counselling.dto;

import java.util.List;
import java.util.Map;

/**
 * All DTOs for the College Counselling APIs.
 * Kept in one file for 2-day build speed — split later if needed.
 */
public class CounsellingDTOs {

    // ── 1. Request: Record a student view ─────────────────────────────────────
    public static class ViewEventRequest {
        public String collegeCode;
        public String courseCode;       // nullable — college-level view if null
        public Double studentPercentile;
        public String category;
        public String gender;
        public String admissionType;
    }

    // ── 2. Request: Record a shortlist ────────────────────────────────────────
    public static class ShortlistRequest {
        public String collegeCode;
        public String courseCode;
        public String courseName;
        public Double studentPercentile;
        public String category;
        public String gender;
        public String admissionType;
        public String capCategoryCode;
    }

    // ── 3. Response: Interested Students (per branch) ─────────────────────────
    public static class InterestedStudentsResponse {
        public String collegeCode;
        public long totalViews;
        public long totalShortlists;
        public List<BranchInterest> byBranch;
        public List<PercentileBand> percentileBands;     // overall percentile distribution
        public List<CategoryCount>  byCategory;          // overall category breakdown

        public InterestedStudentsResponse() {}
    }

    public static class BranchInterest {
        public String courseCode;
        public String courseName;
        public long   shortlists;
        public long   views;
        public double conversionRate;   // shortlists / views * 100
        public List<CategoryCount> byCategory;

        public BranchInterest(String courseCode, String courseName,
                              long shortlists, long views, List<CategoryCount> byCategory) {
            this.courseCode     = courseCode;
            this.courseName     = courseName;
            this.shortlists     = shortlists;
            this.views          = views;
            this.conversionRate = views > 0 ? Math.round(shortlists * 1000.0 / views) / 10.0 : 0;
            this.byCategory     = byCategory;
        }
    }

    public static class PercentileBand {
        public String band;    // "90-100", "80-90", "70-80", "60-70", "50-60", "<50"
        public long   count;

        public PercentileBand(String band, long count) {
            this.band  = band;
            this.count = count;
        }
    }

    public static class CategoryCount {
        public String category;
        public long   count;

        public CategoryCount(String category, long count) {
            this.category = category;
            this.count    = count;
        }
    }

    // ── 4. Response: Target Ranges (who to target, per branch) ────────────────
    public static class TargetRangesResponse {
        public String collegeCode;
        public String collegeName;
        public int    round;
        public List<BranchTargetRange> branches;

        public TargetRangesResponse() {}
    }

    public static class BranchTargetRange {
        public String courseCode;
        public String courseName;
        public String capCategoryCode;  // e.g. GOPENH, GOBCS
        public String category;         // human-readable: OPEN, OBC, SC ...
        public String gender;
        public int    intake;

        // Cutoff data
        public Double lastRoundCutoff;      // actual cutoff from DB (round 4 or latest)
        public Double predictedCutoff;      // ML model prediction
        public String predictionConfidence; // HIGH / MEDIUM / LOW

        // Targeting guidance
        public Double targetMin;    // recommended min percentile to target
        public Double targetMax;    // recommended max percentile to target
        public String rationale;    // human-readable explanation

        // Demand signal from shortlist data
        public long   alreadyShortlisted;  // students already showing interest
        public Double avgInterestedPercentile; // avg percentile of interested students

        public BranchTargetRange() {}
    }

    // ── 5. Response: Cutoff History (per branch, per category, per round) ─────
    public static class CutoffHistoryResponse {
        public String collegeCode;
        public String collegeName;
        public List<BranchCutoffHistory> branches;

        public CutoffHistoryResponse() {}
    }

    public static class BranchCutoffHistory {
        public String courseCode;
        public String courseName;
        public int    intake;
        public List<CategoryCutoffHistory> byCategory;

        public BranchCutoffHistory() {}
        public BranchCutoffHistory(String courseCode, String courseName, int intake) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.intake     = intake;
        }
    }

    public static class CategoryCutoffHistory {
        public String capCategoryCode;
        public String gender;
        public List<RoundCutoff> roundHistory;  // sorted: round 1, 2, 3, 4
        public Double predictedNextCutoff;      // ML prediction
        public String trend;                    // RISING, FALLING, STABLE

        public CategoryCutoffHistory() {}
        public CategoryCutoffHistory(String capCategoryCode, String gender) {
            this.capCategoryCode = capCategoryCode;
            this.gender          = gender;
        }
    }

    public static class RoundCutoff {
        public int    round;
        public Double cutoffPercentile;

        public RoundCutoff(int round, Double cutoffPercentile) {
            this.round             = round;
            this.cutoffPercentile  = cutoffPercentile;
        }
    }

    // ── 6. Response: Target Pool (students to target) ─────────────────────────
    public static class TargetPoolResponse {
        public String collegeCode;
        public String courseCode;
        public String courseName;
        public String capCategoryCode;
        public double targetMin;
        public double targetMax;
        public long   estimatedEligibleInApp;    // students in app matching the filter
        public long   alreadyShortlistedUs;      // of those, already shortlisted this college
        public long   notYetAware;               // estimatedEligible - alreadyShortlisted
        public String note;                      // context message

        public TargetPoolResponse() {}
    }
}
