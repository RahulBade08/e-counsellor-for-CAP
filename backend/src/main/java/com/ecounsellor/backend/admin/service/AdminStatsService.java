package com.ecounsellor.backend.admin.service;

import com.ecounsellor.backend.college.repository.CollegeAccountRepository;
import com.ecounsellor.backend.core.repository.CategoryRepository;
import com.ecounsellor.backend.core.repository.CollegeRepository;
import com.ecounsellor.backend.core.repository.CourseRepository;
import com.ecounsellor.backend.core.repository.CutoffRepository;
import com.ecounsellor.backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates system-wide counts for the Overview page KPIs.
 * All queries are simple JPA count() calls — fast and index-friendly.
 */
@Service
public class AdminStatsService {

    private final CollegeRepository        collegeRepo;
    private final CollegeAccountRepository collegeAccountRepo;
    private final StudentRepository        studentRepo;
    private final CourseRepository         courseRepo;
    private final CutoffRepository         cutoffRepo;
    private final CategoryRepository       categoryRepo;

    public AdminStatsService(
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

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalColleges",      collegeRepo.count());
        stats.put("totalCollegeAccounts", collegeAccountRepo.count());
        stats.put("pendingApprovals",   collegeAccountRepo.countByApprovedFalseAndActiveTrue());
        stats.put("activeStudents",     studentRepo.countByActiveTrue());
        stats.put("suspendedStudents",  studentRepo.countByActiveFalse());
        stats.put("suspendedColleges",  collegeAccountRepo.countByActiveFalse());
        // combine both suspended counts for a single dashboard number
        stats.put("suspendedAccounts",  studentRepo.countByActiveFalse() + collegeAccountRepo.countByActiveFalse());
        stats.put("cutoffRecords",      cutoffRepo.count());
        stats.put("totalCourses",       courseRepo.count());
        stats.put("categories",         categoryRepo.count());
        return stats;
    }
}
