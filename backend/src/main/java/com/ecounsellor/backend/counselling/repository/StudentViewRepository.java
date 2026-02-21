package com.ecounsellor.backend.counselling.repository;

import com.ecounsellor.backend.counselling.entity.StudentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentViewRepository extends JpaRepository<StudentView, Long> {

    // ── Total views for a college ─────────────────────────────────────────────
    long countByCollegeCode(String collegeCode);

    // ── Views grouped by course (branch-level) ────────────────────────────────
    @Query("""
        SELECT v.courseCode, COUNT(v)
        FROM StudentView v
        WHERE v.collegeCode = :collegeCode
          AND v.courseCode IS NOT NULL
        GROUP BY v.courseCode
        ORDER BY COUNT(v) DESC
        """)
    List<Object[]> countViewsByCourse(@Param("collegeCode") String collegeCode);

    // ── Views grouped by category (for a specific branch) ────────────────────
    @Query("""
        SELECT v.category, COUNT(v)
        FROM StudentView v
        WHERE v.collegeCode = :collegeCode
          AND (:courseCode IS NULL OR v.courseCode = :courseCode)
        GROUP BY v.category
        ORDER BY COUNT(v) DESC
        """)
    List<Object[]> countViewsByCategory(
        @Param("collegeCode") String collegeCode,
        @Param("courseCode")  String courseCode);

    // ── Percentile band distribution (for interested students chart) ──────────
    // Returns bands: 90-100, 80-90, 70-80, 60-70, 50-60, <50
    @Query("""
        SELECT
          CASE
            WHEN v.studentPercentile >= 90 THEN '90-100'
            WHEN v.studentPercentile >= 80 THEN '80-90'
            WHEN v.studentPercentile >= 70 THEN '70-80'
            WHEN v.studentPercentile >= 60 THEN '60-70'
            WHEN v.studentPercentile >= 50 THEN '50-60'
            ELSE '<50'
          END as band,
          COUNT(v)
        FROM StudentView v
        WHERE v.collegeCode = :collegeCode
          AND v.studentPercentile IS NOT NULL
        GROUP BY band
        ORDER BY MIN(v.studentPercentile) DESC
        """)
    List<Object[]> percentileBandDistribution(@Param("collegeCode") String collegeCode);

    // ── Count eligible viewers not yet shortlisted (target pool estimate) ─────
    @Query("""
        SELECT COUNT(DISTINCT CONCAT(CAST(v.studentPercentile AS string), v.category, v.gender))
        FROM StudentView v
        WHERE v.collegeCode <> :collegeCode
          AND v.studentPercentile BETWEEN :minPct AND :maxPct
          AND (:category IS NULL OR v.category = :category)
        """)
    long countPotentialTargets(
        @Param("collegeCode") String collegeCode,
        @Param("minPct")      double minPct,
        @Param("maxPct")      double maxPct,
        @Param("category")    String category);
}
