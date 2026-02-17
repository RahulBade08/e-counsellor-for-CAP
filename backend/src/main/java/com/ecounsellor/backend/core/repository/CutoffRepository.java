package com.ecounsellor.backend.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecounsellor.backend.core.entity.Cutoff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {

    // ── No branch/district filter ─────────────────────────────────────────────
    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode   = :capCode
            AND   c.round             = :round
            AND   c.cutoffPercentile <= :percentile
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligible(
            @Param("capCode")     String  capCode,
            @Param("round")       Integer round,
            @Param("percentile")  Double  percentile,
            Pageable pageable);

    // ── Branch filter only ────────────────────────────────────────────────────
    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode        = :capCode
            AND   c.round                  = :round
            AND   c.cutoffPercentile      <= :percentile
            AND   LOWER(co.courseName)     IN :branches
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligibleByBranches(
            @Param("capCode")    String       capCode,
            @Param("round")      Integer      round,
            @Param("percentile") Double       percentile,
            @Param("branches")   List<String> branches,
            Pageable pageable);

    // ── District filter only ──────────────────────────────────────────────────
    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode        = :capCode
            AND   c.round                  = :round
            AND   c.cutoffPercentile      <= :percentile
            AND   LOWER(col.district)      IN :districts
            ORDER BY ABS(c.cutoffPercentile - :percentile) ASC,
                     c.cutoffPercentile DESC
            """)
    Page<Cutoff> findEligibleByDistricts(
            @Param("capCode")    String       capCode,
            @Param("round")      Integer      round,
            @Param("percentile") Double       percentile,
            @Param("districts")  List<String> districts,
            Pageable pageable);

    // ── Both branch + district filters ────────────────────────────────────────
    @Query("""
            SELECT c FROM Cutoff c
            JOIN c.course co JOIN co.college col
            WHERE c.capCategoryCode        = :capCode
            AND   c.round                  = :round
            AND   c.cutoffPercentile      <= :percentile
            AND   LOWER(co.courseName)     IN :branches
            AND   LOWER(col.district)      IN :districts
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
}