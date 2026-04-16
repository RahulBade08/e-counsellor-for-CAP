package com.ecounsellor.backend.counselling.repository;

import com.ecounsellor.backend.counselling.entity.StudentShortlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StudentShortlistRepository extends JpaRepository<StudentShortlist, Long> {

    // ── Total shortlists for a college ────────────────────────────────────────
    long countByCollegeCode(String collegeCode);

    // ── Delete rows when student removes a shortlist ──────────────────────────
    // Matches on collegeCode + courseName (branch display name) + category.
    // NOTE: courseCode column may contain either a short code ("CE") or the
    //       full name ("Civil Engineering") depending on which app path fired
    //       the event. We therefore match on courseName which is always the
    //       human-readable name, and fall back to courseCode when courseName
    //       is blank — this covers both CollegeResultAdapter and StudentAuthService paths.
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND (s.courseName = :courseName
               OR (s.courseName IS NULL AND s.courseCode = :courseName))
          AND (:category IS NULL OR s.category = :category)
        """)
    void deleteByCollegeAndCourseAndCategory(
        @Param("collegeCode") String collegeCode,
        @Param("courseName")  String courseName,
        @Param("category")    String category);

    // ── Shortlists per branch ─────────────────────────────────────────────────
    // FIX (Bug 1): Group by courseName ONLY, not (courseCode, courseName).
    //
    // Root cause of the duplicate: the Android app stores the branch display
    // name in BOTH courseCode and courseName for one path (CollegeResultAdapter),
    // while another path (StudentAuthService.addShortlist) may store a short code
    // in courseCode. This meant "Civil Engineering" appeared once per distinct
    // courseCode value, even though it was the same branch.
    //
    // Grouping solely by courseName collapses all rows for the same branch into
    // one count regardless of what courseCode value was stored.
    @Query("""
        SELECT s.courseName, s.courseName, COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND s.courseName IS NOT NULL
          AND s.courseName <> ''
        GROUP BY s.courseName
        ORDER BY COUNT(s) DESC
        """)
    List<Object[]> countShortlistsByBranch(@Param("collegeCode") String collegeCode);

    // ── Shortlists per branch + category ──────────────────────────────────────
    // FIX (Bug 1 continuation): Same group-by fix as above. r[0] = courseName
    // (used as display key in service), r[1] = courseName (name), r[2] = category,
    // r[3] = count. The service reads indices 0..3 — keeping the shape identical.
    @Query("""
        SELECT s.courseName, s.courseName, s.category, COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND s.courseName IS NOT NULL
          AND s.courseName <> ''
        GROUP BY s.courseName, s.category
        ORDER BY s.courseName, COUNT(s) DESC
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
          AND (:courseCode IS NULL OR s.courseName = :courseCode
               OR s.courseCode = :courseCode)
          AND s.studentPercentile IS NOT NULL
        GROUP BY band
        ORDER BY MIN(s.studentPercentile) DESC
        """)
    List<Object[]> percentileBandDistribution(
        @Param("collegeCode") String collegeCode,
        @Param("courseCode")  String courseCode);

    // ── Avg percentile of students who shortlisted a branch ───────────────────
    // FIX: Match on courseName OR courseCode to handle both storage paths.
    @Query("""
        SELECT AVG(s.studentPercentile)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND (s.courseName = :courseName OR s.courseCode = :courseName)
        """)
    Double avgPercentileForBranch(
        @Param("collegeCode") String collegeCode,
        @Param("courseName")  String courseName);

    // ── Count shortlists already for this college in a percentile+category band ─
    // FIX (Bug 2c): Original query matched courseCode against a short cutoff code
    // like "101", but the student app stores the course *name* in both courseCode
    // and courseName. Matching on courseName (or courseCode fallback) gives correct
    // counts instead of always returning 0.
    @Query("""
        SELECT COUNT(s)
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND (s.courseName = :courseName OR s.courseCode = :courseName)
          AND s.studentPercentile BETWEEN :minPct AND :maxPct
          AND (:category IS NULL OR s.category = :category)
        """)
    long countExistingShortlists(
        @Param("collegeCode") String collegeCode,
        @Param("courseName")  String courseName,
        @Param("minPct")      double minPct,
        @Param("maxPct")      double maxPct,
        @Param("category")    String category);

    // ── Duplicate guard for recordShortlist() ─────────────────────────────────
    // FIX (Bug 3 — double insert): CollegeResultAdapter fires an anonymous
    // POST /event/shortlist, and for logged-in students StudentAuthService also
    // writes a row via POST /student/me/shortlist. Both reach the DB within
    // milliseconds of each other for the same action, doubling every count.
    //
    // This query checks whether an identical row (same college, courseName,
    // percentile rounded to 1dp, category) was inserted in the last 60 seconds.
    // If yes, recordShortlist() skips the insert.
    //
    // 60 seconds is wide enough to absorb any network latency between the two
    // calls, and narrow enough that a genuine re-shortlist after removal won't
    // be blocked (the student would have to remove and re-add within 60 s, which
    // is an edge case we accept).
    @Query("""
        SELECT COUNT(s) > 0
        FROM StudentShortlist s
        WHERE s.collegeCode = :collegeCode
          AND (s.courseName = :courseName OR s.courseCode = :courseName)
          AND (:percentile IS NULL OR ABS(COALESCE(s.studentPercentile, 0) - :percentile) < 0.1)
          AND (:category IS NULL OR s.category = :category)
          AND s.shortlistedAt >= :cutoff
        """)
    boolean existsRecentDuplicate(
        @Param("collegeCode") String collegeCode,
        @Param("courseName")  String courseName,
        @Param("percentile")  Double percentile,
        @Param("category")    String category,
        @Param("cutoff")      java.time.LocalDateTime cutoff);

    default boolean existsRecentDuplicate(
            String collegeCode, String courseName,
            Double percentile, String category) {
        return existsRecentDuplicate(
            collegeCode, courseName, percentile, category,
            java.time.LocalDateTime.now().minusSeconds(60));
    }
}