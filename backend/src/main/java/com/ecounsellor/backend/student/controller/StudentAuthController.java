package com.ecounsellor.backend.student.controller;

import com.ecounsellor.backend.student.dto.StudentAuthDTOs.*;
import com.ecounsellor.backend.student.service.StudentAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Student Authentication API
 *
 * PUBLIC (no token needed):
 *   POST /api/student/auth/register   — create account
 *   POST /api/student/auth/login      — login, get JWT token
 *
 * PROTECTED (requires Authorization: Bearer <token>):
 *   GET  /api/student/me              — get own profile
 *   PUT  /api/student/me              — update preferences
 */
@RestController
@CrossOrigin(origins = "*")
public class StudentAuthController {

    private final StudentAuthService service;

    public StudentAuthController(StudentAuthService service) {
        this.service = service;
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────
    /**
     * POST /api/student/auth/register
     * Body: {
     *   "name": "Rahul Patil",
     *   "phone": "9876543210",
     *   "password": "rahul123",
     *   "cetAppNumber": "24-CET-012345",   ← optional
     *   "cetPercentile": 82.4,
     *   "category": "OBC",
     *   "gender": "GENERAL",
     *   "admissionType": "STATE"
     * }
     * Response: { "token": "eyJ...", "role": "STUDENT", "profile": {...} }
     */
    @PostMapping("/api/student/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            AuthResponse resp = service.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    /**
     * POST /api/student/auth/login
     * Body: { "phone": "9876543210", "password": "rahul123" }
     * Response: { "token": "eyJ...", "role": "STUDENT", "profile": {...} }
     */
    @PostMapping("/api/student/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            AuthResponse resp = service.login(req);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── GET OWN PROFILE ───────────────────────────────────────────────────────
    /**
     * GET /api/student/me
     * Header: Authorization: Bearer <token>
     * Response: StudentProfile
     */
    @GetMapping("/api/student/me")
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        try {
            String phone = (String) request.getAttribute("currentUser");
            return ResponseEntity.ok(service.getProfile(phone));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────────
    /**
     * PUT /api/student/me
     * Header: Authorization: Bearer <token>
     * Body: { "cetPercentile": 83.1, "category": "OBC", "gender": "GENERAL",
     *         "admissionType": "STATE",
     *         "preferredBranches": "[\"Computer Science\",\"IT\"]",
     *         "preferredDistricts": "[\"Pune\",\"Nashik\"]" }
     */
    @PutMapping("/api/student/me")
    public ResponseEntity<?> updateMe(@RequestBody UpdateProfileRequest req,
                                      HttpServletRequest request) {
        try {
            String phone = (String) request.getAttribute("currentUser");
            return ResponseEntity.ok(service.updateProfile(phone, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
