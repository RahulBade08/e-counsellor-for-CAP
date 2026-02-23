package com.ecounsellor.backend.student.service;

import com.ecounsellor.backend.admin.util.JwtUtil;
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

    private final StudentRepository repo;
    private final PasswordEncoder   encoder;
    private final JwtUtil           jwtUtil;
    private final ObjectMapper      objectMapper = new ObjectMapper();

    public StudentAuthService(StudentRepository repo,
                              PasswordEncoder encoder,
                              JwtUtil jwtUtil) {
        this.repo    = repo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
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
    // Adds one college to the student's shortlist. Ignores duplicates.
    public StudentProfile addShortlist(String phone, ShortlistItem item) {
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ShortlistItem> list = parseShortlist(s.getShortlistedColleges());

        // Deduplicate: skip if already shortlisted
        boolean exists = list.stream().anyMatch(i ->
                i.collegeCode.equals(item.collegeCode) &&
                i.courseName.equals(item.courseName));
        if (!exists) {
            list.add(item);
            s.setShortlistedColleges(toJson(list));
            repo.save(s);
        }

        return new StudentProfile(s);
    }

    // ── REMOVE SHORTLIST ──────────────────────────────────────────────────────
    public StudentProfile removeShortlist(String phone, RemoveShortlistRequest req) {
        Student s = repo.findByPhone(phone)
            .orElseThrow(() -> new RuntimeException("Student not found"));

        List<ShortlistItem> list = parseShortlist(s.getShortlistedColleges());
        list.removeIf(i ->
                i.collegeCode.equals(req.collegeCode) &&
                i.courseName.equals(req.courseName));

        s.setShortlistedColleges(toJson(list));
        repo.save(s);

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
}