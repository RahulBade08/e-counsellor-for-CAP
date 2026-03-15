package com.ecounsellor.backend.college.repository;

import com.ecounsellor.backend.college.entity.CollegeAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollegeAccountRepository extends JpaRepository<CollegeAccount, Long> {

    Optional<CollegeAccount> findByEmail(String email);

    Optional<CollegeAccount> findByCollegeCode(String collegeCode);

    boolean existsByEmail(String email);

    boolean existsByCollegeCode(String collegeCode);

    // ── Admin panel queries ───────────────────────────────────────────────────

    /** Pending: registered but not yet approved */
    List<CollegeAccount> findByApprovedFalseAndActiveTrue();

    /** Count of pending approvals — used in Overview KPI */
    long countByApprovedFalseAndActiveTrue();

    /** Count of suspended accounts */
    long countByActiveFalse();
}
