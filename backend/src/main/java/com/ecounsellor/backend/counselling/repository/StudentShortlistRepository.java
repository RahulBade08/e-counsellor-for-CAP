package com.ecounsellor.backend.counselling.repository;

import com.ecounsellor.backend.counselling.entity.StudentShortlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentShortlistRepository extends JpaRepository<StudentShortlist, Long> {

    // ── Total shortlists for a college ────────────────────────────────────────
    long countByCollegeCode(String collegeCode);

    // ── Shortlists per branch ─────────────────────────────────────────────────
    @Query("""
        SELECT s.courseCode, s.courseName, COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
        GROUP BY s.courseCode, s.courseName
        ORDER BY COUNT(s) DESC
        """)
    List<Object[]> countShortlistsByBranch(@Param("collegeCode") String collegeCode);

    // ── Shortlists per branch + category ──────────────────────────────────────
    @Query("""
        SELECT s.courseCode, s.courseName, s.category, COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
        GROUP BY s.courseCode, s.courseName, s.category
        ORDER BY s.courseCode, COUNT(s) DESC
        """)
    List<Object[]> countShortlistsByBranchAndCategory(@Param("collegeCode") String collegeCode);

    // ── Percentile band distribution for shortlists ───────────────────────────
    @Query("""
        SELECT
          CASE
            WHEN s.studentPercentile >= 90 THEN '90-100'
            WHEN s.studentPercentile >= 80 THEN '80-90'
            WHEN s.studentPercentile >= 70 THEN '70-80'
            WHEN s.studentPercentile >= 60 THEN '60-70'
            WHEN s.studentPercentile >= 50 THEN '50-60'
            ELSE '<50'
          END as band,
          COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND (:courseCode IS NULL OR s.courseCode = :courseCode)
          AND s.studentPercentile IS NOT NULL
        GROUP BY band
        ORDER BY MIN(s.studentPercentile) DESC
        """)
    List<Object[]> percentileBandDistribution(
        @Param("collegeCode") String collegeCode,
        @Param("courseCode")  String courseCode);

    // ── Avg percentile of students who shortlisted a branch ───────────────────
    @Query("""
        SELECT AVG(s.studentPercentile)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND s.courseCode  = :courseCode
        """)
    Double avgPercentileForBranch(
        @Param("collegeCode") String collegeCode,
        @Param("courseCode")  String courseCode);

    // ── Count shortlists already for this college in a percentile+category band ─
    @Query("""
        SELECT COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND s.courseCode  = :courseCode
          AND s.studentPercentile BETWEEN :minPct AND :maxPct
          AND (:category IS NULL OR s.category = :category)
        """)
    long countExistingShortlists(
        @Param("collegeCode") String collegeCode,
        @Param("courseCode")  String courseCode,
        @Param("minPct")      double minPct,
        @Param("maxPct")      double maxPct,
        @Param("category")    String category);
}
