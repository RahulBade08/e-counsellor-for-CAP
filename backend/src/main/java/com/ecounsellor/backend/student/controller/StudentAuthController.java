package com.ecounsellor.backend.student.controller;

import com.ecounsellor.backend.student.dto.StudentAuthDTOs.*;
import com.ecounsellor.backend.student.service.StudentAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class StudentAuthController {

    private final StudentAuthService service;

    public StudentAuthController(StudentAuthService service) {
        this.service = service;
    }

    // ── PUBLIC ────────────────────────────────────────────────────────────────

    @PostMapping("/api/student/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.register(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/api/student/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(service.login(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ── PROTECTED (JWT required) ──────────────────────────────────────────────

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

    // ── SHORTLIST ─────────────────────────────────────────────────────────────

    /**
     * POST /api/student/me/shortlist
     * Body: { "collegeCode": "06298", "courseName": "Computer Engineering",
     *         "collegeName": "Sinhgad...", "district": "Pune", "university": "SPPU" }
     * Adds one college to the student's permanent shortlist.
     * Returns updated StudentProfile.
     */
    @PostMapping("/api/student/me/shortlist")
    public ResponseEntity<?> addShortlist(@RequestBody ShortlistItem item,
                                          HttpServletRequest request) {
        try {
            String phone = (String) request.getAttribute("currentUser");
            return ResponseEntity.ok(service.addShortlist(phone, item));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * DELETE /api/student/me/shortlist
     * Body: { "collegeCode": "06298", "courseName": "Computer Engineering" }
     * Removes one college from the shortlist.
     * Returns updated StudentProfile.
     */
    @DeleteMapping("/api/student/me/shortlist")
    public ResponseEntity<?> removeShortlist(@RequestBody RemoveShortlistRequest req,
                                             HttpServletRequest request) {
        try {
            String phone = (String) request.getAttribute("currentUser");
            return ResponseEntity.ok(service.removeShortlist(phone, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}