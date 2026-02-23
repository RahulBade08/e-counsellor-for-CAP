package com.ecounsellor.backend.student.dto;

import com.ecounsellor.backend.student.entity.Student;

public class StudentAuthDTOs {

    // ── REGISTER REQUEST ──────────────────────────────────────────────────────
    public static class RegisterRequest {
        public String name;
        public String phone;
        public String password;
        public String cetAppNumber;
        public Double cetPercentile;
        public String category;
        public String gender;
        public String admissionType;
    }

    // ── LOGIN REQUEST ─────────────────────────────────────────────────────────
    public static class LoginRequest {
        public String phone;
        public String password;
    }

    // ── UPDATE PROFILE REQUEST ────────────────────────────────────────────────
    public static class UpdateProfileRequest {
        public String name;
        public Double cetPercentile;
        public String category;
        public String gender;
        public String admissionType;
        public String preferredBranches;
        public String preferredDistricts;
    }

    // ── SHORTLIST ITEM — one college the student has shortlisted ──────────────
    // Sent as body to POST /api/student/me/shortlist
    // Also returned inside StudentProfile.shortlistedColleges (as JSON array stored in DB)
    public static class ShortlistItem {
        public String collegeCode;
        public String courseName;
        public String collegeName;   // display only — stored for convenience
        public String district;      // display only
        public String university;    // display only

        public ShortlistItem() {}
        public ShortlistItem(String collegeCode, String courseName,
                             String collegeName, String district, String university) {
            this.collegeCode  = collegeCode;
            this.courseName   = courseName;
            this.collegeName  = collegeName;
            this.district     = district;
            this.university   = university;
        }
    }

    // ── REMOVE SHORTLIST REQUEST ──────────────────────────────────────────────
    public static class RemoveShortlistRequest {
        public String collegeCode;
        public String courseName;
    }

    // ── AUTH RESPONSE (register + login) ──────────────────────────────────────
    public static class AuthResponse {
        public String         token;
        public String         role;
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
        public String shortlistedColleges;  // JSON array string — Android parses with Gson

        public StudentProfile() {}
        public StudentProfile(Student s) {
            this.id                  = s.getId();
            this.name                = s.getName();
            this.phone               = s.getPhone();
            this.cetAppNumber        = s.getCetAppNumber();
            this.cetPercentile       = s.getCetPercentile();
            this.category            = s.getCategory();
            this.gender              = s.getGender();
            this.admissionType       = s.getAdmissionType();
            this.preferredBranches   = s.getPreferredBranches();
            this.preferredDistricts  = s.getPreferredDistricts();
            this.shortlistedColleges = s.getShortlistedColleges();
        }
    }

    // ── ERROR RESPONSE ────────────────────────────────────────────────────────
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}