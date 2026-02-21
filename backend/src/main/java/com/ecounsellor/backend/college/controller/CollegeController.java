package com.ecounsellor.backend.college.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.college.dto.CollegeDTO;
import com.ecounsellor.backend.college.service.CollegeService;

@RestController
@RequestMapping("/api/college")
@CrossOrigin(origins = "*")
public class CollegeController {

    private final CollegeService service;

    public CollegeController(CollegeService service) {
        this.service = service;
    }

    // ── TEST ──────────────────────────────────────────────────────────────────
    @GetMapping("/test")
    public String test() {
        return "College Panel is working!";
    }

    // ── GET ALL DISTRICTS (for student dropdown) ──────────────────────────────
    // GET /api/college/districts
    // Response: ["Ahmednagar", "Amravati", "Aurangabad", ...]
    @GetMapping("/districts")
    public ResponseEntity<List<String>> getDistricts() {
        return ResponseEntity.ok(service.getAllDistricts());
    }
    
    //*** ── GET ALL BRANCHES (distinct course names from DB) ──────────────────────
    // GET /api/college/branches
    // Response: ["Artificial Intelligence", "Computer Engineering", ...]
    // Used by Android app branch dropdown — replaces hardcoded group labels.
    @GetMapping("/branches")
    public ResponseEntity<List<String>> getBranches() {
        return ResponseEntity.ok(service.getAllBranches());
    }


    // ── GET ALL REGIONS ───────────────────────────────────────────────────────
    // GET /api/college/regions
    @GetMapping("/regions")
    public ResponseEntity<List<String>> getRegions() {
        return ResponseEntity.ok(service.getAllRegions());
    }

    // ── GET COLLEGES BY DISTRICT(S) — MAIN FEATURE ───────────────────────────
    // GET /api/college/by-district?districts=Pune&districts=Nashik
    // or  /api/college/by-district?districts=Pune  (single district)
    //
    // Response: full college list with courses, for all selected districts
    @GetMapping("/by-district")
    public ResponseEntity<List<CollegeDTO>> getByDistricts(
            @RequestParam List<String> districts) {

        if (districts == null || districts.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<CollegeDTO> colleges = service.getByDistricts(districts);
        return ResponseEntity.ok(colleges);
    }
    
    
    // ── GET ALL COLLEGES ──────────────────────────────────────────────────────
    // GET /api/college/all
    @GetMapping("/all")
    public ResponseEntity<List<CollegeDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // ── GET COLLEGE BY ID ─────────────────────────────────────────────────────
    // GET /api/college/42
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET COLLEGE BY CODE ───────────────────────────────────────────────────
    // GET /api/college/code/06155
    @GetMapping("/code/{collegeCode}")
    public ResponseEntity<?> getByCode(@PathVariable String collegeCode) {
        try {
            return ResponseEntity.ok(service.getByCode(collegeCode));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── SEARCH BY NAME ────────────────────────────────────────────────────────
    // GET /api/college/search?name=Sinhgad
    @GetMapping("/search")
    public ResponseEntity<List<CollegeDTO>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(service.searchByName(name));
    }

    // ── COUNT ─────────────────────────────────────────────────────────────────
    // GET /api/college/count
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of("count", service.count()));
    }
}