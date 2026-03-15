package com.ecounsellor.backend.counselling.controller;

import com.ecounsellor.backend.counselling.dto.CounsellingDTOs.*;
import com.ecounsellor.backend.counselling.service.CounsellingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * College Counselling API — UPDATED with college ownership enforcement.
 *
 * PUBLIC (no auth — Android app fires these silently):
 *   POST /api/counselling/event/view          — record a student view
 *   POST /api/counselling/event/shortlist      — record a student shortlist
 *
 * COLLEGE DASHBOARD (requires JWT with role=COLLEGE):
 *   GET  /api/counselling/{collegeCode}/interested      — Feature 1
 *   GET  /api/counselling/{collegeCode}/target-pool     — Feature 2
 *   GET  /api/counselling/{collegeCode}/target-ranges   — Feature 3
 *   GET  /api/counselling/{collegeCode}/cutoff-history  — Feature 4
 *
 * OWNERSHIP RULE:
 *   A college's JWT subject = their collegeCode.
 *   The controller verifies that the path {collegeCode} matches the JWT subject.
 *   This prevents College A from reading College B's data.
 *
 * REPLACE your existing CounsellingController.java with this file.
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

    @PostMapping("/event/view")
    public ResponseEntity<Void> recordView(@RequestBody ViewEventRequest req) {
        try { service.recordView(req); } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    @PostMapping("/event/shortlist")
    public ResponseEntity<Void> recordShortlist(@RequestBody ShortlistRequest req) {
        try { service.recordShortlist(req); } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPER: Ownership check
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ensures the college in the path is the same as the logged-in college.
     * Returns an error response if the check fails, null if OK.
     */
    private ResponseEntity<?> checkOwnership(String pathCollegeCode, HttpServletRequest request) {
        String jwtCollegeCode = (String) request.getAttribute("currentUser");
        String role           = (String) request.getAttribute("currentRole");

        // Admins can see any college's data
        if ("ADMIN".equals(role)) return null;

        if (jwtCollegeCode == null || !jwtCollegeCode.equals(pathCollegeCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\":\"You can only view your own college data.\"}");
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 1 — INTERESTED STUDENTS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{collegeCode}/interested")
    public ResponseEntity<?> getInterestedStudents(
            @PathVariable String collegeCode,
            HttpServletRequest request) {
        ResponseEntity<?> ownershipError = checkOwnership(collegeCode, request);
        if (ownershipError != null) return ownershipError;
        return ResponseEntity.ok(service.getInterestedStudents(collegeCode));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 2 — TARGET POOL
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{collegeCode}/target-pool")
    public ResponseEntity<?> getTargetPool(
            @PathVariable String collegeCode,
            @RequestParam String  courseCode,
            @RequestParam String  capCategoryCode,
            @RequestParam(defaultValue = "4") int round,
            HttpServletRequest request) {
        ResponseEntity<?> ownershipError = checkOwnership(collegeCode, request);
        if (ownershipError != null) return ownershipError;
        return ResponseEntity.ok(
            service.getTargetPool(collegeCode, courseCode, capCategoryCode, round));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 3 — TARGET RANGES
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{collegeCode}/target-ranges")
    public ResponseEntity<?> getTargetRanges(
            @PathVariable String collegeCode,
            @RequestParam(defaultValue = "4") int round,
            HttpServletRequest request) {
        ResponseEntity<?> ownershipError = checkOwnership(collegeCode, request);
        if (ownershipError != null) return ownershipError;
        return ResponseEntity.ok(service.getTargetRanges(collegeCode, round));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 4 — CUTOFF HISTORY + ML PREDICTION
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{collegeCode}/cutoff-history")
    public ResponseEntity<?> getCutoffHistory(
            @PathVariable String collegeCode,
            HttpServletRequest request) {
        ResponseEntity<?> ownershipError = checkOwnership(collegeCode, request);
        if (ownershipError != null) return ownershipError;
        return ResponseEntity.ok(service.getCutoffHistory(collegeCode));
    }
}