package com.ecounsellor.backend.college.dto;

import com.ecounsellor.backend.college.entity.CollegeAccount;
import com.ecounsellor.backend.core.entity.College;

/**
 * All DTOs for College Account Auth & Profile APIs.
 */
public class CollegeAuthDTOs {

    // ── REGISTER REQUEST ──────────────────────────────────────────────────────
    /**
     * POST /api/college/auth/register
     *
     * College self-registers. Account is created with approved=false.
     * Admin must approve before the college can log in.
     *
     * (Or you can auto-approve by setting approved=true in service — your call.)
     */
    public static class CollegeRegisterRequest {
        public String collegeCode;          // DTE college code — must exist in colleges table
        public String email;                // login email
        public String password;             // min 6 chars
        public String contactPersonName;    // HOD / Principal name
        public String contactPhone;         // optional
    }

    // ── LOGIN REQUEST ─────────────────────────────────────────────────────────
    /**
     * POST /api/college/auth/login
     */
    public static class CollegeLoginRequest {
        public String email;
        public String password;
    }

    // ── AUTH RESPONSE ─────────────────────────────────────────────────────────
    /**
     * Returned from both register and login.
     * Contains JWT token + college profile.
     */
    public static class CollegeAuthResponse {
        public String         token;
        public String         role = "COLLEGE";
        public CollegeProfile profile;

        public CollegeAuthResponse(String token, CollegeProfile profile) {
            this.token   = token;
            this.profile = profile;
        }
    }

    // ── COLLEGE PROFILE ───────────────────────────────────────────────────────
    /**
     * Returned in auth response and from GET /api/college/auth/me.
     * Combines CollegeAccount (auth info) + College (DTE data).
     */
    public static class CollegeProfile {
        // From CollegeAccount
        public Long   accountId;
        public String collegeCode;
        public String email;
        public String contactPersonName;
        public String contactPhone;
        public boolean approved;

        // From colleges table (DTE data)
        public String collegeName;
        public String university;
        public String fundingType;
        public Boolean isAutonomous;
        public String minorityStatus;
        public Integer totalIntake;
        public String address;
        public String region;
        public String district;

        public CollegeProfile() {}

        /** Build from account only (when college record not found) */
        public CollegeProfile(CollegeAccount acc) {
            this.accountId          = acc.getId();
            this.collegeCode        = acc.getCollegeCode();
            this.email              = acc.getEmail();
            this.contactPersonName  = acc.getContactPersonName();
            this.contactPhone       = acc.getContactPhone();
            this.approved           = acc.isApproved();
        }

        /** Build from account + DTE college record */
        public CollegeProfile(CollegeAccount acc, College col) {
            this(acc);
            if (col != null) {
                this.collegeName    = col.getCollegeName();
                this.university     = col.getCourseUniversity();
                this.fundingType    = col.getFundingType();
                this.isAutonomous   = col.getIsAutonomous();
                this.minorityStatus = col.getMinorityStatus();
                this.totalIntake    = col.getTotalIntake();
                this.address        = col.getAddress();
                this.region         = col.getRegion();
                this.district       = col.getDistrict();
            }
        }
    }

    // ── UPDATE PROFILE REQUEST ────────────────────────────────────────────────
    /**
     * PUT /api/college/auth/me
     * College can update their contact info (not collegeCode or email — those are fixed).
     */
    public static class CollegeUpdateProfileRequest {
        public String contactPersonName;
        public String contactPhone;
        public String newPassword;          // optional — only if they want to change password
        public String currentPassword;      // required if newPassword is provided
    }

    // ── ERROR RESPONSE ────────────────────────────────────────────────────────
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}
