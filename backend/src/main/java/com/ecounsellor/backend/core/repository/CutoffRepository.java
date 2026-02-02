package com.ecounsellor.backend.core.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecounsellor.backend.core.entity.Category;
import com.ecounsellor.backend.core.entity.Cutoff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {

	

	@Query("""
		    SELECT c FROM Cutoff c
		    WHERE c.category = :category
		    AND c.gender = :gender
		    AND c.round = :round
		    AND c.cutoffPercentile <= :percentile
		    AND (
		        :branch IS NULL
		        OR LOWER(c.course.courseName)
		           LIKE LOWER(CONCAT('%', :branch, '%'))
		    )
		    ORDER BY ABS(c.cutoffPercentile - :percentile) ASC, c.cutoffPercentile DESC

		""")
		Page<Cutoff> findEligibleCutoffsPaged(
		    @Param("category") Category category,
		    @Param("gender") String gender,
		    @Param("round") Integer round,
		    @Param("percentile") Double percentile,
		    @Param("branch") String branch,
		    Pageable pageable
		);



}
