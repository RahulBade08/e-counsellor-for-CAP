package com.ecounsellor.backend.student.dto;

/**
 * All request/response DTOs for student auth.
 * Kept in one file for build speed.
 */
public class StudentAuthDTOs {

    // ── REGISTER REQUEST ──────────────────────────────────────────────────────
    // Student fills this on the registration screen
    public static class RegisterRequest {
        public String name;             // Full name
        public String phone;            // 10-digit mobile number (we store as-is)
        public String password;         // Min 6 chars — student chooses this
        public String cetAppNumber;     // From hall ticket — optional but useful
        public Double cetPercentile;    // From result card — e.g. 82.4
        public String category;         // OPEN / OBC / SC / ST / NT1 / NT2 / NT3 / EWS / TFWS
        public String gender;           // GENERAL / LADIES
        public String admissionType;    // STATE / HOME / OTHER
    }

    // ── LOGIN REQUEST ─────────────────────────────────────────────────────────
    public static class LoginRequest {
        public String phone;
        public String password;
    }

    // ── UPDATE PROFILE REQUEST ────────────────────────────────────────────────
    // After login, student can update their preferences
    public static class UpdateProfileRequest {
        public String name;
        public Double cetPercentile;    // in case they re-check after result
        public String category;
        public String gender;
        public String admissionType;
        public String preferredBranches;    // JSON string e.g. ["Computer Science"]
        public String preferredDistricts;   // JSON string e.g. ["Pune","Nashik"]
    }

    // ── AUTH RESPONSE (returned on register + login) ──────────────────────────
    public static class AuthResponse {
        public String token;            // JWT — store in Android EncryptedSharedPreferences
        public String role;             // always "STUDENT"
        public StudentProfile profile;

        public AuthResponse(String token, StudentProfile profile) {
            this.token   = token;
            this.role    = "STUDENT";
            this.profile = profile;
        }
    }

    // ── STUDENT PROFILE (returned in auth response + GET /me) ─────────────────
    public static class StudentProfile {
        public Long   id;
        public String name;
        public String phone;
        public String cetAppNumber;
        public Double cetPercentile;
        public String category;
        public String gender;
        public String admissionType;
        public String preferredBranches;
        public String preferredDistricts;

        public StudentProfile() {}
        public StudentProfile(com.ecounsellor.backend.student.entity.Student s) {
            this.id                 = s.getId();
            this.name               = s.getName();
            this.phone              = s.getPhone();
            this.cetAppNumber       = s.getCetAppNumber();
            this.cetPercentile      = s.getCetPercentile();
            this.category           = s.getCategory();
            this.gender             = s.getGender();
            this.admissionType      = s.getAdmissionType();
            this.preferredBranches  = s.getPreferredBranches();
            this.preferredDistricts = s.getPreferredDistricts();
        }
    }

    // ── ERROR RESPONSE ────────────────────────────────────────────────────────
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}
