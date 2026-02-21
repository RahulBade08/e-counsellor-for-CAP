package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.Cutoff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {

    // ══════════════════════════════════════════════════════════════════════════
    // EXISTING QUERIES — unchanged
    // ══════════════════════════════════════════════════════════════════════════

    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode  = :capCode
            AND   c.round            = :round
            AND   c.cutoffPercentile <= :percentile
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligible(
            @Param("capCode")    String  capCode,
            @Param("round")      Integer round,
            @Param("percentile") Double  percentile,
            Pageable pageable);

    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode  = :capCode
            AND   c.round            = :round
            AND   c.cutoffPercentile <= :percentile
            AND   co.courseName      IN :branches
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligibleByBranches(
            @Param("capCode")    String       capCode,
            @Param("round")      Integer      round,
            @Param("percentile") Double       percentile,
            @Param("branches")   List<String> branches,
            Pageable pageable);

    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode   = :capCode
            AND   c.round             = :round
            AND   c.cutoffPercentile  <= :percentile
            AND   LOWER(col.district) IN :districts
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligibleByDistricts(
            @Param("capCode")    String       capCode,
            @Param("round")      Integer      round,
            @Param("percentile") Double       percentile,
            @Param("districts")  List<String> districts,
            Pageable pageable);

    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode   = :capCode
            AND   c.round             = :round
            AND   c.cutoffPercentile  <= :percentile
            AND   co.courseName       IN :branches
            AND   LOWER(col.district) IN :districts
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligibleByBranchesAndDistricts(
            @Param("capCode")    String       capCode,
            @Param("round")      Integer      round,
            @Param("percentile") Double       percentile,
            @Param("branches")   List<String> branches,
            @Param("districts")  List<String> districts,
            Pageable pageable);

    // ══════════════════════════════════════════════════════════════════════════
    // NEW — College Counselling Queries
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Full cutoff history for a college — all branches, all categories, all rounds.
     * Returns: courseCode, courseName, capCategoryCode, gender, round, cutoffPercentile, intake
     * Used by: /api/counselling/{collegeCode}/cutoff-history
     */
    @Query("""
        SELECT co.courseCode, co.courseName,
               c.capCategoryCode, c.gender,
               c.round, c.cutoffPercentile, co.intake
        FROM Cutoff c
        JOIN c.course co
        JOIN co.college col
        WHERE col.collegeCode = :collegeCode
        ORDER BY co.courseName, c.capCategoryCode, c.round
        """)
    List<Object[]> findCutoffHistoryByCollegeCode(@Param("collegeCode") String collegeCode);

    /**
     * Latest cutoffs for a college at a specific round — for target range calculation.
     * Returns: courseCode, courseName, capCategoryCode, gender, cutoffPercentile, intake
     * Used by: /api/counselling/{collegeCode}/target-ranges
     */
    @Query("""
        SELECT co.courseCode, co.courseName,
               c.capCategoryCode, c.gender,
               c.cutoffPercentile, co.intake
        FROM Cutoff c
        JOIN c.course co
        JOIN co.college col
        WHERE col.collegeCode = :collegeCode
          AND c.round         = :round
        ORDER BY co.courseName, c.capCategoryCode
        """)
    List<Object[]> findLatestCutoffsByCollegeAndRound(
        @Param("collegeCode") String collegeCode,
        @Param("round")       int    round);

    /**
     * Get cutoffs for a specific branch+category for ML prediction input.
     * Returns the most recent round cutoff for that combination.
     * Used internally by CounsellingService to call ML for prediction.
     */
    @Query("""
        SELECT c.cutoffPercentile
        FROM Cutoff c
        JOIN c.course co
        JOIN co.college col
        WHERE col.collegeCode    = :collegeCode
          AND co.courseCode      = :courseCode
          AND c.capCategoryCode  = :capCategoryCode
        ORDER BY c.round DESC
        """)
    List<Double> findCutoffForPrediction(
        @Param("collegeCode")    String collegeCode,
        @Param("courseCode")     String courseCode,
        @Param("capCategoryCode") String capCategoryCode);
}
