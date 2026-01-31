package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.Cutoff;
import com.ecounsellor.backend.core.entity.Category;
import com.ecounsellor.backend.core.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {

    // 🔹 Used for prediction (core query)
    List<Cutoff> findByCourseAndCategoryAndRound(
            Course course,
            Category category,
            Integer round
    );

    // 🔹 Student recommendation (top colleges)
    @Query("""
        SELECT c
        FROM Cutoff c
        WHERE c.category = :category
          AND c.round = :round
          AND c.cutoffPercentile <= :percentile
        ORDER BY c.cutoffPercentile DESC
    """)
    List<Cutoff> findEligibleCutoffs(
            @Param("category") Category category,
            @Param("round") Integer round,
            @Param("percentile") Double percentile
    );

    // 🔹 Trend analysis (for ML later)
    @Query("""
        SELECT c
        FROM Cutoff c
        WHERE c.course = :course
          AND c.category = :category
        ORDER BY c.round ASC
    """)
    List<Cutoff> findTrendData(
            @Param("course") Course course,
            @Param("category") Category category
    );
}
