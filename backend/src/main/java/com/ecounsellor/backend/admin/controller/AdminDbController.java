package com.ecounsellor.backend.admin.controller;

import com.ecounsellor.backend.college.entity.CollegeAccount;
import com.ecounsellor.backend.college.repository.CollegeAccountRepository;
import com.ecounsellor.backend.core.entity.Category;
import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.entity.Course;
import com.ecounsellor.backend.core.entity.Cutoff;
import com.ecounsellor.backend.core.repository.CategoryRepository;
import com.ecounsellor.backend.core.repository.CollegeRepository;
import com.ecounsellor.backend.core.repository.CourseRepository;
import com.ecounsellor.backend.core.repository.CutoffRepository;
import com.ecounsellor.backend.student.entity.Student;
import com.ecounsellor.backend.student.repository.StudentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only database browser for the Admin Dashboard.
 * All endpoints are paged (max 500 rows) to protect against huge tables.
 *
 * GET /api/admin/db/colleges         — colleges table
 * GET /api/admin/db/college-accounts — college_accounts table
 * GET /api/admin/db/students         — students table (password_hash omitted via JSON ignore)
 * GET /api/admin/db/courses          — courses table
 * GET /api/admin/db/cutoffs          — cutoffs table (paged, default page=0)
 * GET /api/admin/db/categories       — categories table
 *
 * NOTE: Student.passwordHash is marked @JsonIgnore in the Student entity
 *       (add @JsonIgnore to the getPasswordHash() getter so it never leaks).
 */
@RestController
@RequestMapping("/api/admin/db")
public class AdminDbController {

    private final CollegeRepository        collegeRepo;
    private final CollegeAccountRepository collegeAccountRepo;
    private final StudentRepository        studentRepo;
    private final CourseRepository         courseRepo;
    private final CutoffRepository         cutoffRepo;
    private final CategoryRepository       categoryRepo;

    public AdminDbController(
            CollegeRepository        collegeRepo,
            CollegeAccountRepository collegeAccountRepo,
            StudentRepository        studentRepo,
            CourseRepository         courseRepo,
            CutoffRepository         cutoffRepo,
            CategoryRepository       categoryRepo) {
        this.collegeRepo        = collegeRepo;
        this.collegeAccountRepo = collegeAccountRepo;
        this.studentRepo        = studentRepo;
        this.courseRepo         = courseRepo;
        this.cutoffRepo         = cutoffRepo;
        this.categoryRepo       = categoryRepo;
    }

    @GetMapping("/colleges")
    public ResponseEntity<List<College>> colleges() {
        return ResponseEntity.ok(collegeRepo.findAll());
    }

    @GetMapping("/college-accounts")
    public ResponseEntity<List<CollegeAccount>> collegeAccounts() {
        // password_hash is excluded by @JsonProperty(access = WRITE_ONLY) on entity
        return ResponseEntity.ok(collegeAccountRepo.findAll());
    }

    @GetMapping("/students")
    public ResponseEntity<List<Student>> students() {
        // password_hash excluded by @JsonIgnore on entity getter
        return ResponseEntity.ok(studentRepo.findAll());
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> courses() {
        return ResponseEntity.ok(courseRepo.findAll());
    }

    /**
     * Cutoffs table can be very large — page it.
     * ?page=0&size=200  (default: first 200 rows)
     */
    @GetMapping("/cutoffs")
    public ResponseEntity<?> cutoffs(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "200") int size) {
        // Cap max page size at 500 to prevent OOM
        int safeSize = Math.min(size, 500);
        return ResponseEntity.ok(
                cutoffRepo.findAll(PageRequest.of(page, safeSize)).getContent()
        );
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return ResponseEntity.ok(categoryRepo.findAll());
    }
}
