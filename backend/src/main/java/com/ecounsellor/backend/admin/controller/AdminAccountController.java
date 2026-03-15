package com.ecounsellor.backend.admin.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.admin.service.AdminAccountService;
import com.ecounsellor.backend.college.entity.CollegeAccount;
import com.ecounsellor.backend.student.entity.Student;

/**
 * Account management for the Admin Dashboard.
 *
 * College accounts:
 *   GET  /api/admin/accounts/colleges              — list all college accounts
 *   PUT  /api/admin/accounts/approve-college/{id}  — approve pending registration
 *   PUT  /api/admin/accounts/suspend-college/{id}  — suspend active account
 *   PUT  /api/admin/accounts/activate-college/{id} — reactivate suspended account
 *   DELETE /api/admin/accounts/college/{id}        — permanently delete account
 *
 * Student accounts:
 *   GET  /api/admin/accounts/students              — list all students
 *   PUT  /api/admin/accounts/suspend-student/{id}  — suspend student
 *   PUT  /api/admin/accounts/activate-student/{id} — reactivate student
 *   DELETE /api/admin/accounts/student/{id}        — permanently delete student
 */
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    // ── College accounts ──────────────────────────────────────────────────────

    @GetMapping("/colleges")
    public ResponseEntity<List<CollegeAccount>> allColleges() {
        return ResponseEntity.ok(service.getAllCollegeAccounts());
    }

    @PutMapping("/approve-college/{id}")
    public ResponseEntity<?> approveCollege(@PathVariable Long id, Principal principal) {
        try {
            CollegeAccount acc = service.approveCollege(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "College account approved", "collegeCode", acc.getCollegeCode()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/suspend-college/{id}")
    public ResponseEntity<?> suspendCollege(@PathVariable Long id, Principal principal) {
        try {
            service.suspendCollege(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "College account suspended"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/activate-college/{id}")
    public ResponseEntity<?> activateCollege(@PathVariable Long id, Principal principal) {
        try {
            service.reactivateCollege(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "College account reactivated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/college/{id}")
    public ResponseEntity<?> deleteCollege(@PathVariable Long id, Principal principal) {
        try {
            service.deleteCollegeAccount(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "College account deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Student accounts ──────────────────────────────────────────────────────

    @GetMapping("/students")
    public ResponseEntity<List<Student>> allStudents() {
        return ResponseEntity.ok(service.getAllStudents());
    }

    @PutMapping("/suspend-student/{id}")
    public ResponseEntity<?> suspendStudent(@PathVariable Long id, Principal principal) {
        try {
            service.suspendStudent(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "Student suspended"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/activate-student/{id}")
    public ResponseEntity<?> activateStudent(@PathVariable Long id, Principal principal) {
        try {
            service.activateStudent(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "Student activated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/student/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id, Principal principal) {
        try {
            service.deleteStudent(id, actorName(principal));
            return ResponseEntity.ok(Map.of("message", "Student deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String actorName(Principal principal) {
        return principal != null ? principal.getName() : "admin";
    }
}
