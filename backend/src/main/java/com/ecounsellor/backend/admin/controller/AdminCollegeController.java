package com.ecounsellor.backend.admin.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecounsellor.backend.college.dto.CollegeDTO;
import com.ecounsellor.backend.college.service.CollegeService;

@RestController
@RequestMapping("/api/admin/college")
public class AdminCollegeController {

    private final CollegeService service;

    public AdminCollegeController(CollegeService service) {
        this.service = service;
    }

    // 🔹 TEST ENDPOINT
    @GetMapping("/test")
    public String test() {
        return "Admin College Management is working!";
    }

    // 🔹 CREATE COLLEGE
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CollegeDTO dto) {
        try {
            CollegeDTO created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 UPDATE COLLEGE
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody CollegeDTO dto) {
        try {
            CollegeDTO updated = service.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 DELETE COLLEGE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("message", "College deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}