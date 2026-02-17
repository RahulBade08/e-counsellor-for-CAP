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
@CrossOrigin(origins = "*") // Configure as needed
public class CollegeController {

    private final CollegeService service;

    public CollegeController(CollegeService service) {
        this.service = service;
    }

    // 🔹 TEST ENDPOINT
    @GetMapping("/test")
    public String test() {
        return "College Panel is working!";
    }

    // 🔹 GET ALL COLLEGES
    @GetMapping("/all")
    public ResponseEntity<List<CollegeDTO>> getAll() {
        List<CollegeDTO> colleges = service.getAll();
        return ResponseEntity.ok(colleges);
    }

    // 🔹 GET COLLEGE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            CollegeDTO college = service.getById(id);
            return ResponseEntity.ok(college);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 GET COLLEGE BY CODE
    @GetMapping("/code/{collegeCode}")
    public ResponseEntity<?> getByCode(@PathVariable String collegeCode) {
        try {
            CollegeDTO college = service.getByCode(collegeCode);
            return ResponseEntity.ok(college);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 SEARCH COLLEGES BY NAME
    @GetMapping("/search")
    public ResponseEntity<List<CollegeDTO>> searchByName(@RequestParam String name) {
        List<CollegeDTO> colleges = service.searchByName(name);
        return ResponseEntity.ok(colleges);
    }

    // 🔹 GET COLLEGE COUNT
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        long count = service.count();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
