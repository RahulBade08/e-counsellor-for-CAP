package com.ecounsellor.backend.college.service;

import com.ecounsellor.backend.admin.util.JwtUtil;
import com.ecounsellor.backend.college.dto.CollegeAuthDTOs.*;
import com.ecounsellor.backend.college.entity.CollegeAccount;
import com.ecounsellor.backend.college.repository.CollegeAccountRepository;
import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.repository.CollegeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CollegeAuthService {

    private final CollegeAccountRepository accountRepo;
    private final CollegeRepository        collegeRepo;
    private final PasswordEncoder          encoder;
    private final JwtUtil                  jwtUtil;

    public CollegeAuthService(CollegeAccountRepository accountRepo,
                              CollegeRepository        collegeRepo,
                              PasswordEncoder          encoder,
                              JwtUtil                  jwtUtil) {
        this.accountRepo = accountRepo;
        this.collegeRepo = collegeRepo;
        this.encoder     = encoder;
        this.jwtUtil     = jwtUtil;
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    /**
     * Self-registration by a college.
     *
     * APPROVAL POLICY (choose one — currently auto-approves for easy dev):
     *   - To require admin approval: set acc.setApproved(false) [default]
     *   - To auto-approve:          set acc.setApproved(true)
     *
     * If you require approval, the login method will throw an error
     * until admin sets approved=true in the DB or via admin panel.
     */
    public CollegeAuthResponse register(CollegeRegisterRequest req) {
        // ── Validation ─────────────────────────────────────────────────────
        if (req.collegeCode == null || req.collegeCode.isBlank())
            throw new RuntimeException("College code is required");
        if (req.email == null || req.email.isBlank())
            throw new RuntimeException("Email is required");
        if (req.password == null || req.password.length() < 6)
            throw new RuntimeException("Password must be at least 6 characters");

        String code  = req.collegeCode.trim();
        String email = req.email.trim().toLowerCase();

        // ── Uniqueness checks ──────────────────────────────────────────────
        if (accountRepo.existsByCollegeCode(code))
            throw new RuntimeException("An account for college code " + code +
                    " already exists. Contact admin if you lost access.");
        if (accountRepo.existsByEmail(email))
            throw new RuntimeException("This email is already registered.");

        // ── Verify college exists in the DTE database ──────────────────────
        College college = collegeRepo.findByCollegeCode(code)
            .orElseThrow(() -> new RuntimeException(
                "College code '" + code + "' not found in database. " +
                "Please double-check your college code."));

        // ── Create account ─────────────────────────────────────────────────
        CollegeAccount acc = new CollegeAccount();
        acc.setCollegeCode(code);
        acc.setEmail(email);
        acc.setPasswordHash(encoder.encode(req.password));
        acc.setContactPersonName(req.contactPersonName != null ? req.contactPersonName.trim() : null);
        acc.setContactPhone(req.contactPhone != null ? req.contactPhone.trim() : null);

        // Requires admin approval before college can log in
        acc.setApproved(false);

        accountRepo.save(acc);

        // ── Issue token (only if auto-approved) ────────────────────────────
        if (acc.isApproved()) {
            String token = jwtUtil.generateToken(code, "COLLEGE");
            return new CollegeAuthResponse(token, new CollegeProfile(acc, college));
        } else {
            // Return profile without token — college must wait for approval
            CollegeAuthResponse resp = new CollegeAuthResponse(null, new CollegeProfile(acc, college));
            resp.role = "PENDING";
            return resp;
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    public CollegeAuthResponse login(CollegeLoginRequest req) {
        if (req.email == null || req.password == null)
            throw new RuntimeException("Email and password are required");

        String email = req.email.trim().toLowerCase();

        CollegeAccount acc = accountRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException(
                "No account found for this email. Please register first."));

        if (!acc.isActive())
            throw new RuntimeException("Account is deactivated. Contact admin.");

        if (!acc.isApproved())
            throw new RuntimeException(
                "Your account is pending admin approval. You will be notified once approved.");

        if (!encoder.matches(req.password, acc.getPasswordHash()))
            throw new RuntimeException("Incorrect password.");

        // Update last login
        acc.setLastLoginAt(LocalDateTime.now());
        accountRepo.save(acc);

        // Fetch DTE college details
        College college = collegeRepo.findByCollegeCode(acc.getCollegeCode()).orElse(null);

        String token = jwtUtil.generateToken(acc.getCollegeCode(), "COLLEGE");
        return new CollegeAuthResponse(token, new CollegeProfile(acc, college));
    }

    // ── GET PROFILE ───────────────────────────────────────────────────────────

    public CollegeProfile getProfile(String collegeCode) {
        CollegeAccount acc = accountRepo.findByCollegeCode(collegeCode)
            .orElseThrow(() -> new RuntimeException("College account not found"));
        College college = collegeRepo.findByCollegeCode(collegeCode).orElse(null);
        return new CollegeProfile(acc, college);
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────────

    public CollegeProfile updateProfile(String collegeCode, CollegeUpdateProfileRequest req) {
        CollegeAccount acc = accountRepo.findByCollegeCode(collegeCode)
            .orElseThrow(() -> new RuntimeException("College account not found"));

        if (req.contactPersonName != null && !req.contactPersonName.isBlank())
            acc.setContactPersonName(req.contactPersonName.trim());

        if (req.contactPhone != null && !req.contactPhone.isBlank())
            acc.setContactPhone(req.contactPhone.trim());

        // Password change — requires current password
        if (req.newPassword != null && !req.newPassword.isBlank()) {
            if (req.currentPassword == null || req.currentPassword.isBlank())
                throw new RuntimeException("Current password is required to set a new password.");
            if (!encoder.matches(req.currentPassword, acc.getPasswordHash()))
                throw new RuntimeException("Current password is incorrect.");
            if (req.newPassword.length() < 6)
                throw new RuntimeException("New password must be at least 6 characters.");
            acc.setPasswordHash(encoder.encode(req.newPassword));
        }

        accountRepo.save(acc);
        College college = collegeRepo.findByCollegeCode(collegeCode).orElse(null);
        return new CollegeProfile(acc, college);
    }
}