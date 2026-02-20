package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.Cutoff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {

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

    // Exact IN match — branches are already expanded to exact DB names by BranchGroups.expand()
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

    // LOWER() on both sides — districts passed as lowercase, handles any DB case inconsistency
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
}