package com.ecounsellor.backend.student.service;


import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecounsellor.backend.core.entity.Category;
import com.ecounsellor.backend.core.entity.Cutoff;
import com.ecounsellor.backend.core.repository.CategoryRepository;
import com.ecounsellor.backend.core.repository.CutoffRepository;
import com.ecounsellor.backend.student.dto.StudentPredictionRequest;
import com.ecounsellor.backend.student.dto.StudentPredictionResponse;

@Service
public class StudentPredictionService {

    private final CutoffRepository cutoffRepository;
    private final CategoryRepository categoryRepository;

    public StudentPredictionService(
            CutoffRepository cutoffRepository,
            CategoryRepository categoryRepository
    ) {
        this.cutoffRepository = cutoffRepository;
        this.categoryRepository = categoryRepository;
    }
    
    private String calculateRisk(Double studentPercentile, Double cutoffPercentile) {

        double diff = studentPercentile - cutoffPercentile;

        if (diff >= 1.0) {
            return "SAFE";
        } else if (diff >= 0.3) {
            return "MODERATE";
        } else {
            return "RISKY";
        }
    }



    public Page<StudentPredictionResponse> predictColleges(
            StudentPredictionRequest request,
            int page,
            int size
    ) {

        Integer round = request.getRound() != null ? request.getRound() : 4;
        String gender = request.getGender() != null
                ? request.getGender().toUpperCase()
                : "GENERAL";

        Category category = categoryRepository
                .findByCategoryName(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        Pageable pageable = PageRequest.of(page, size);

        Page<Cutoff> cutoffsPage =
                cutoffRepository.findEligibleCutoffsPaged(
                        category,
                        gender,
                        round,
                        request.getPercentile(),
                        pageable
                );

        // sort only page content by closeness
        List<StudentPredictionResponse> sorted =
                cutoffsPage.getContent().stream()
                        .sorted(Comparator.comparingDouble(
                                c -> Math.abs(request.getPercentile() - c.getCutoffPercentile())
                        ))
                        .map(c -> new StudentPredictionResponse(
                                c.getCourse().getCollege().getCollegeName(),
                                c.getCourse().getCourseName(),
                                c.getCutoffPercentile(),
                                c.getRound(),
                                calculateRisk(request.getPercentile(), c.getCutoffPercentile())
                        ))
                        .toList();

        return new PageImpl<>(
                sorted,
                pageable,
                cutoffsPage.getTotalElements()
        );

    }
}
