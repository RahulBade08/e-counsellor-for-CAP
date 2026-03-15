package com.ecounsellor.backend.college.controller;

import com.ecounsellor.backend.college.dto.CollegeAuthDTOs.*;
import com.ecounsellor.backend.college.service.CollegeAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * College Account Auth & Profile API
 *
 * PUBLIC (no token):
 *   POST /api/college/auth/register  — college self-registers
 *   POST /api/college/auth/login     — college logs in, gets JWT
 *
 * PROTECTED (requires JWT with role=COLLEGE):
 *   GET  /api/college/auth/me        — get college profile
 *   PUT  /api/college/auth/me        — update contact info / password
 *
 * The JWT subject is collegeCode (e.g. "06155").
 * The counselling dashboard APIs at /api/counselling/{collegeCode}/**
 * are protected and will verify the JWT role is COLLEGE.
 *
 * SECURITY NOTE:
 * The CounsellingController currently accepts .authenticated() for all roles.
 * You should add a check there (or via @PreAuthorize) that colleges can only
 * query THEIR OWN collegeCode. The dashboard does this automatically since
 * it reads the collegeCode from the JWT subject.
 */
@RestController
@RequestMapping("/api/college/auth")
@CrossOrigin(origins = "*")
public class CollegeAuthController {

    private final CollegeAuthService service;

    public CollegeAuthController(CollegeAuthService service) {
        this.service = service;
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    /**
     * POST /api/college/auth/register
     *
     * Body: {
     *   "collegeCode": "06155",
     *   "email": "principal@vjti.ac.in",
     *   "password": "securepass",
     *   "contactPersonName": "Dr. Ramesh Patil",
     *   "contactPhone": "9876543210"
     * }
     *
     * Returns: { token, role, profile } on success (if auto-approved)
     *          { role: "PENDING", profile } if awaiting admin approval
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CollegeRegisterRequest req) {
        try {
            CollegeAuthResponse resp = service.register(req);
            HttpStatus status = "PENDING".equals(resp.role)
                ? HttpStatus.ACCEPTED      // 202 — registered but pending
                : HttpStatus.CREATED;      // 201 — registered and approved
            return ResponseEntity.status(status).body(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/college/auth/login
     *
     * Body: { "email": "principal@vjti.ac.in", "password": "securepass" }
     *
     * Returns: { token, role: "COLLEGE", profile }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody CollegeLoginRequest req) {
        try {
            return ResponseEntity.ok(service.login(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET PROFILE ───────────────────────────────────────────────────────────

    /**
     * GET /api/college/auth/me
     * Requires: Authorization: Bearer <college_jwt>
     *
     * Returns full college profile (account + DTE data).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        try {
            String collegeCode = (String) request.getAttribute("currentUser");
            return ResponseEntity.ok(service.getProfile(collegeCode));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────────

    /**
     * PUT /api/college/auth/me
     * Requires: Authorization: Bearer <college_jwt>
     *
     * Body: { "contactPersonName": "...", "contactPhone": "...",
     *         "currentPassword": "...", "newPassword": "..." }
     *
     * Returns updated profile.
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody CollegeUpdateProfileRequest req,
                                      HttpServletRequest request) {
        try {
            String collegeCode = (String) request.getAttribute("currentUser");
            return ResponseEntity.ok(service.updateProfile(collegeCode, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}