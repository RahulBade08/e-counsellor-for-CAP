package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {

    Optional<College> findByCollegeCode(String collegeCode);

    Optional<College> findByCollegeNameIgnoreCase(String collegeName);

    // ── Single district ──────────────────────────────────────────────────────
    List<College> findByDistrictIgnoreCase(String district);

    // ── Multiple districts (student selects 1 or more) ──────────────────────
    @Query("SELECT c FROM College c WHERE LOWER(c.district) IN :districts ORDER BY c.collegeName")
    List<College> findByDistrictIn(@Param("districts") List<String> districts);

    // ── All distinct districts (for frontend dropdown) ───────────────────────
    @Query("SELECT DISTINCT c.district FROM College c WHERE c.district IS NOT NULL AND c.district <> '' ORDER BY c.district")
    List<String> findAllDistricts();

    // ── All distinct regions ─────────────────────────────────────────────────
    @Query("SELECT DISTINCT c.region FROM College c WHERE c.region IS NOT NULL AND c.region <> '' ORDER BY c.region")
    List<String> findAllRegions();

    // ── Search by name (case-insensitive) ────────────────────────────────────
    List<College> findByCollegeNameContainingIgnoreCase(String name);
}