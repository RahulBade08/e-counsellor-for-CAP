package com.ecounsellor.backend.student.service;

import com.ecounsellor.backend.admin.util.JwtUtil;
import com.ecounsellor.backend.counselling.entity.StudentShortlist;
import com.ecounsellor.backend.counselling.repository.StudentShortlistRepository;
import com.ecounsellor.backend.student.dto.StudentAuthDTOs.*;
import com.ecounsellor.backend.student.entity.Student;
import com.ecounsellor.backend.student.repository.StudentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentAuthService {

    private final StudentRepository          repo;
    private final PasswordEncoder            encoder;
    private final JwtUtil                    jwtUtil;
    private final StudentShortlistRepository shortlistRepo;
    private final ObjectMapper               objectMapper = new ObjectMapper();

    public StudentAuthService(StudentRepository          repo,
                              PasswordEncoder            encoder,
                              JwtUtil                    jwtUtil,
                              StudentShortlistRepository shortlistRepo) {
        this.repo          = repo;
        this.encoder       = encoder;
        this.jwtUtil       = jwtUtil;
        this.shortlistRepo = shortlistRepo;
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────
    public AuthResponse register(RegisterRequest req) {
        if (req.phone == null || req.phone.isBlank())
            throw new RuntimeException("Phone number is required");
        if (req.password == null || req.password.length() < 6)
            throw new RuntimeException("Password must be at least 6 characters");
        if (req.name == null || req.name.isBlank())
            throw new RuntimeException("Name is required");
        if (req.cetPercentile == null)
            throw new RuntimeException("CET percentile is required");
        if (req.cetPercentile < 0 || req.cetPercentile > 100)
            throw new RuntimeException("Percentile must be between 0 and 100");

        String phone = normalizePhone(req.phone);
        if (repo.existsByPhone(phone))
            throw new RuntimeException("Phone number already registered. Please login.");

        Student s = new Student();
        s.setPhone(phone);
        s.setPasswordHash(encoder.encode(req.password));
        s.setName(req.name.trim());
        s.setCetAppNumber(req.cetAppNumber != null ? req.cetAppNumber.trim() : null);
        s.setCetPercentile(req.cetPercentile);
        s.setCategory(req.category);
        s.setGender(req.gender);
        s.setAdmissionType(req.admissionType != null ? req.admissionType : "STATE");

        Student saved = repo.save(s);
        String token = jwtUtil.generateToken(phone, "STUDENT");
        return new AuthResponse(token, new StudentProfile(saved));
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    public AuthResponse login(LoginRequest req) {
        if (req.phone == null || req.password == null)
            throw new RuntimeException("Phone and password are required");

        String phone = normalizePhone(req.phone);
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("No account found for this phone number"));

        if (!s.isActive())
            throw new RuntimeException("Account is deactivated. Contact support.");
        if (!encoder.matches(req.password, s.getPasswordHash()))
            throw new RuntimeException("Incorrect password");

        s.setLastLoginAt(LocalDateTime.now());
        repo.save(s);

        String token = jwtUtil.generateToken(phone, "STUDENT");
        return new AuthResponse(token, new StudentProfile(s));
    }

    // ── GET PROFILE ───────────────────────────────────────────────────────────
    public StudentProfile getProfile(String phone) {
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        return new StudentProfile(s);
    }

    // ── UPDATE PROFILE ────────────────────────────────────────────────────────
    public StudentProfile updateProfile(String phone, UpdateProfileRequest req) {
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        if (req.name != null && !req.name.isBlank()) s.setName(req.name.trim());
        if (req.cetPercentile != null)  s.setCetPercentile(req.cetPercentile);
        if (req.category != null)       s.setCategory(req.category);
        if (req.gender != null)         s.setGender(req.gender);
        if (req.admissionType != null)  s.setAdmissionType(req.admissionType);
        if (req.preferredBranches != null)  s.setPreferredBranches(req.preferredBranches);
        if (req.preferredDistricts != null) s.setPreferredDistricts(req.preferredDistricts);

        return new StudentProfile(repo.save(s));
    }

    // ── ADD SHORTLIST ─────────────────────────────────────────────────────────
    // Writes to:
    //   1. students.shortlisted_colleges (JSON) — Android app persistent list
    //   2. student_shortlists table            — college counselling dashboard
    //
    // FIX (Bug 3 — double insert): CollegeResultAdapter fires an anonymous
    // POST /event/shortlist at the same time this method inserts into
    // student_shortlists. The existsRecentDuplicate guard in
    // StudentShortlistRepository (60-second window) prevents the anonymous
    // event from creating a second row. However to be safe, this method
    // also checks if a row with the same fingerprint exists in the last
    // 30 seconds before inserting, so that the two paths don't race.
    public StudentProfile addShortlist(String phone, ShortlistItem item) {
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ShortlistItem> list = parseShortlist(s.getShortlistedColleges());

        boolean exists = list.stream().anyMatch(i ->
                i.collegeCode.equals(item.collegeCode) &&
                i.courseName.equals(item.courseName));

        if (!exists) {
            list.add(item);
            s.setShortlistedColleges(toJson(list));
            repo.save(s);

            // Guard: skip insert if the anonymous event already wrote this row
            // within the last 30 seconds (tighter window than the 60s in recordShortlist)
            boolean alreadyRecorded = shortlistRepo.existsRecentDuplicate(
                item.collegeCode, item.courseName,
                s.getCetPercentile(), s.getCategory(),
                LocalDateTime.now().minusSeconds(30));

            if (!alreadyRecorded) {
                StudentShortlist sl = new StudentShortlist();
                sl.setCollegeCode(item.collegeCode);
                // FIX: always store human-readable name in courseCode column too,
                // so both columns are consistent and grouping by courseName works
                sl.setCourseCode(item.courseName != null ? item.courseName : "");
                sl.setCourseName(item.courseName);
                sl.setStudentPercentile(s.getCetPercentile());
                sl.setCategory(s.getCategory());
                sl.setGender(s.getGender());
                sl.setAdmissionType(s.getAdmissionType());
                // FIX (Bug 2a): use corrected deriveCapCategoryCode
                sl.setCapCategoryCode(
                    deriveCapCategoryCode(s.getCategory(), s.getAdmissionType(), s.getGender()));
                shortlistRepo.save(sl);
            }
        }

        return new StudentProfile(s);
    }

    // ── REMOVE SHORTLIST ──────────────────────────────────────────────────────
    // Removes from both:
    //   1. students.shortlisted_colleges (JSON) — Android app persistent list
    //   2. student_shortlists table            — college counselling dashboard
    public StudentProfile removeShortlist(String phone, RemoveShortlistRequest req) {
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        // 1. Remove from JSON column on student record
        List<ShortlistItem> list = parseShortlist(s.getShortlistedColleges());
        list.removeIf(i ->
                i.collegeCode.equals(req.collegeCode) &&
                i.courseName.equals(req.courseName));
        s.setShortlistedColleges(toJson(list));
        repo.save(s);

        // 2. Remove from dashboard table
        // FIX: pass courseName (human-readable name like "Civil Engineering")
        // because that is what is stored in the courseName column and what
        // deleteByCollegeAndCourseAndCategory now matches on.
        shortlistRepo.deleteByCollegeAndCourseAndCategory(
                req.collegeCode,
                req.courseName != null ? req.courseName : "",
                s.getCategory());

        return new StudentProfile(s);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private List<ShortlistItem> parseShortlist(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ShortlistItem>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(List<ShortlistItem> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String normalizePhone(String phone) {
        String p = phone.replaceAll("[\\s\\-]", "");
        if (p.startsWith("+91") && p.length() == 13) p = p.substring(3);
        else if (p.startsWith("91") && p.length() == 12) p = p.substring(2);
        if (p.length() != 10) throw new RuntimeException("Enter a valid 10-digit phone number");
        return p;
    }

    /**
     * FIX (Bug 2a) — deriveCapCategoryCode: corrected suffix logic.
     *
     * Maharashtra CAP code format: [G|L] + CATEGORY + [S|H|O]
     *   Prefix: G = General (State-level), L = Ladies OR Home-university quota
     *   Suffix: S = State, H = Home University, O = Other State
     *
     * Previous bug for OPEN category:
     *   Old code: prefix + "OPEN" + (isLadies ? "S" : "H")
     *   This was backwards — for a general male STATE student it produced "GOPENH"
     *   (Home University quota) instead of "GOPENS" (State quota).
     *
     * Correct logic:
     *   Suffix depends on admissionType, NOT on gender.
     *   S = STATE (most students), H = HOME university, O = OTHER state.
     *   Ladies prefix (L) is independent and applies for LADIES gender.
     *
     * All categories use the same suffix derivation — OPEN had a special case
     * that was simply wrong.
     */
    private String deriveCapCategoryCode(String category, String admissionType, String gender) {
        if (category == null) return "GOPENS";
        if ("EWS".equals(category))  return "EWS";
        if ("TFWS".equals(category)) return "TFWS";

        boolean isLadies = "LADIES".equalsIgnoreCase(gender);
        String prefix = isLadies ? "L" : "G";

        // Suffix is determined solely by admission type
        String suffix;
        if ("HOME".equalsIgnoreCase(admissionType))        suffix = "H";
        else if ("OTHER".equalsIgnoreCase(admissionType))  suffix = "O";
        else                                                suffix = "S"; // STATE (default)

        String normalizedCat = switch (category.toUpperCase()) {
            case "OPEN"               -> "OPEN";
            case "OBC"                -> "OBC";
            case "SC"                 -> "SC";
            case "ST"                 -> "ST";
            case "NT1", "NT-1"        -> "NT1";
            case "NT2", "NT-2"        -> "NT2";
            case "NT3", "NT-3"        -> "NT3";
            // VJ (Vimukta Jati) is its own category — not an alias for NT1
            case "VJ"                 -> "VJ";
            case "SBC"                -> "OBC";
            default                   -> "OPEN";
        };

        return prefix + normalizedCat + suffix;
    }
}