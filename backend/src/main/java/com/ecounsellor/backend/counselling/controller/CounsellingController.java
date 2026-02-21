package com.ecounsellor.backend.counselling.controller;

import com.ecounsellor.backend.counselling.dto.CounsellingDTOs.*;
import com.ecounsellor.backend.counselling.service.CounsellingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * College Counselling API
 *
 * PUBLIC (no auth — Android app fires these silently):
 *   POST /api/counselling/event/view          — record a student view
 *   POST /api/counselling/event/shortlist      — record a student shortlist
 *
 * COLLEGE DASHBOARD (will add JWT auth later):
 *   GET  /api/counselling/{collegeCode}/interested      — Feature 1
 *   GET  /api/counselling/{collegeCode}/target-pool     — Feature 2
 *   GET  /api/counselling/{collegeCode}/target-ranges   — Feature 3
 *   GET  /api/counselling/{collegeCode}/cutoff-history  — Feature 4
 */
@RestController
@RequestMapping("/api/counselling")
@CrossOrigin(origins = "*")
public class CounsellingController {

    private final CounsellingService service;

    public CounsellingController(CounsellingService service) {
        this.service = service;
    }

    // ── Test ──────────────────────────────────────────────────────────────────
    @GetMapping("/test")
    public String test() {
        return "College Counselling API is working!";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EVENT TRACKING — Android app calls these silently (no auth needed)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Called when student opens CollegeDetailFragment.
     * Fire-and-forget — always return 200 even if it fails.
     *
     * POST /api/counselling/event/view
     * Body: { "collegeCode":"06155", "courseCode":"101",
     *          "studentPercentile":82.4, "category":"OPEN",
     *          "gender":"GENERAL", "admissionType":"STATE" }
     */
    @PostMapping("/event/view")
    public ResponseEntity<Void> recordView(@RequestBody ViewEventRequest req) {
        try { service.recordView(req); } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    /**
     * Called when student taps shortlist/save button on a college result card.
     *
     * POST /api/counselling/event/shortlist
     * Body: { "collegeCode":"06155", "courseCode":"101", "courseName":"Computer Engineering",
     *          "studentPercentile":82.4, "category":"OPEN", "gender":"GENERAL",
     *          "admissionType":"STATE", "capCategoryCode":"GOPENH" }
     */
    @PostMapping("/event/shortlist")
    public ResponseEntity<Void> recordShortlist(@RequestBody ShortlistRequest req) {
        try { service.recordShortlist(req); } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 1 — INTERESTED STUDENTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/counselling/{collegeCode}/interested
     *
     * Returns:
     * - Total views and shortlists for this college
     * - Per-branch shortlist count + category breakdown
     * - Percentile band distribution of interested students
     * - Category breakdown overall
     *
     * Example: GET /api/counselling/06155/interested
     */
    @GetMapping("/{collegeCode}/interested")
    public ResponseEntity<InterestedStudentsResponse> getInterestedStudents(
            @PathVariable String collegeCode) {
        return ResponseEntity.ok(service.getInterestedStudents(collegeCode));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 2 — TARGET POOL (students to target, for a specific branch)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/counselling/{collegeCode}/target-pool
     *     ?courseCode=101&capCategoryCode=GOPENH&round=2
     *
     * Returns how many app-users match the eligible percentile range
     * for this branch+category, and how many already shortlisted this college.
     *
     * Example: GET /api/counselling/06155/target-pool?courseCode=101&capCategoryCode=GOPENH&round=2
     */
    @GetMapping("/{collegeCode}/target-pool")
    public ResponseEntity<TargetPoolResponse> getTargetPool(
            @PathVariable String collegeCode,
            @RequestParam String  courseCode,
            @RequestParam String  capCategoryCode,
            @RequestParam(defaultValue = "4") int round) {
        return ResponseEntity.ok(
            service.getTargetPool(collegeCode, courseCode, capCategoryCode, round));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 3 — TARGET RANGES (cutoff range to target students, all branches)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/counselling/{collegeCode}/target-ranges?round=4
     *
     * Returns for each branch+category:
     * - Last actual cutoff
     * - ML predicted cutoff for this year
     * - Recommended target percentile range (min/max)
     * - Human-readable rationale
     * - Demand signal (students already interested)
     *
     * Example: GET /api/counselling/06155/target-ranges?round=4
     */
    @GetMapping("/{collegeCode}/target-ranges")
    public ResponseEntity<TargetRangesResponse> getTargetRanges(
            @PathVariable String collegeCode,
            @RequestParam(defaultValue = "4") int round) {
        return ResponseEntity.ok(service.getTargetRanges(collegeCode, round));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 4 — CUTOFF HISTORY + ML PREDICTION (branch-wise)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/counselling/{collegeCode}/cutoff-history
     *
     * Returns for each branch → each category → each round:
     * - Historical cutoffs (all rounds in DB)
     * - ML-predicted next cutoff
     * - Trend: RISING / FALLING / STABLE
     *
     * Example: GET /api/counselling/06155/cutoff-history
     */
    @GetMapping("/{collegeCode}/cutoff-history")
    public ResponseEntity<CutoffHistoryResponse> getCutoffHistory(
            @PathVariable String collegeCode) {
        return ResponseEntity.ok(service.getCutoffHistory(collegeCode));
    }
}
