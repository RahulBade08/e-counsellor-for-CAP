package com.ecounsellor.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecounsellor.backend.entity.Cutoff;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {
    List<Cutoff> findByBranchAndCategoryAndRound(String branch, String category, Integer round);
}
