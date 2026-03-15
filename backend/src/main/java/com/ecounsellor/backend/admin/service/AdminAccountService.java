package com.ecounsellor.backend.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecounsellor.backend.college.entity.CollegeAccount;
import com.ecounsellor.backend.college.repository.CollegeAccountRepository;
import com.ecounsellor.backend.student.entity.Student;
import com.ecounsellor.backend.student.repository.StudentRepository;

/**
 * Handles all account management actions that the admin panel performs:
 *  - Approve a pending college account
 *  - Suspend / reactivate a college account
 *  - Suspend / reactivate a student account
 *
 * Every action writes an audit log via AdminLogService.
 */
@Service
public class AdminAccountService {

    private final CollegeAccountRepository collegeAccountRepo;
    private final StudentRepository        studentRepo;
    private final AdminLogService          logService;

    public AdminAccountService(
            CollegeAccountRepository collegeAccountRepo,
            StudentRepository studentRepo,
            AdminLogService logService) {
        this.collegeAccountRepo = collegeAccountRepo;
        this.studentRepo        = studentRepo;
        this.logService         = logService;
    }

    // ── College accounts ──────────────────────────────────────────────────────

    public List<CollegeAccount> getAllCollegeAccounts() {
        return collegeAccountRepo.findAll();
    }

    public CollegeAccount approveCollege(Long id, String adminUsername) {
        CollegeAccount acc = collegeAccountRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("College account not found: " + id));
        acc.setApproved(true);
        acc.setActive(true);
        CollegeAccount saved = collegeAccountRepo.save(acc);
        logService.success(adminUsername,
                "Approved college account: " + acc.getCollegeCode() + " (" + acc.getEmail() + ")");
        return saved;
    }

    public CollegeAccount suspendCollege(Long id, String adminUsername) {
        CollegeAccount acc = collegeAccountRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("College account not found: " + id));
        acc.setActive(false);
        CollegeAccount saved = collegeAccountRepo.save(acc);
        logService.warn(adminUsername,
                "Suspended college account: " + acc.getCollegeCode() + " (" + acc.getEmail() + ")");
        return saved;
    }

    public CollegeAccount reactivateCollege(Long id, String adminUsername) {
        CollegeAccount acc = collegeAccountRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("College account not found: " + id));
        acc.setActive(true);
        CollegeAccount saved = collegeAccountRepo.save(acc);
        logService.info(adminUsername,
                "Reactivated college account: " + acc.getCollegeCode());
        return saved;
    }

    public void deleteCollegeAccount(Long id, String adminUsername) {
        CollegeAccount acc = collegeAccountRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("College account not found: " + id));
        String code = acc.getCollegeCode();
        collegeAccountRepo.deleteById(id);
        logService.error(adminUsername, "Deleted college account: " + code);
    }

    // ── Student accounts ──────────────────────────────────────────────────────

    public List<Student> getAllStudents() {
        return studentRepo.findAll();
    }

    public Student suspendStudent(Long id, String adminUsername) {
        Student s = studentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
        s.setActive(false);
        Student saved = studentRepo.save(s);
        logService.warn(adminUsername,
                "Suspended student: " + s.getName() + " (" + s.getPhone() + ")");
        return saved;
    }

    public Student activateStudent(Long id, String adminUsername) {
        Student s = studentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
        s.setActive(true);
        Student saved = studentRepo.save(s);
        logService.info(adminUsername,
                "Activated student: " + s.getName() + " (" + s.getPhone() + ")");
        return saved;
    }

    public void deleteStudent(Long id, String adminUsername) {
        Student s = studentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found: " + id));
        String name = s.getName();
        studentRepo.deleteById(id);
        logService.error(adminUsername, "Deleted student account: " + name);
    }
}
