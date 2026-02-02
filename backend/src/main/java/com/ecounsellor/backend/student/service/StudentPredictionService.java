package com.ecounsellor.backend.student.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecounsellor.backend.core.entity.Category;
import com.ecounsellor.backend.core.entity.Cutoff;
import com.ecounsellor.backend.core.ml.MLClient;
import com.ecounsellor.backend.core.repository.CategoryRepository;
import com.ecounsellor.backend.core.repository.CutoffRepository;
import com.ecounsellor.backend.student.dto.StudentPredictionRequest;
import com.ecounsellor.backend.student.dto.StudentPredictionResponse;

@Service
public class StudentPredictionService {

    private final CutoffRepository cutoffRepository;
    private final CategoryRepository categoryRepository;
    private final MLClient mlClient;

    public StudentPredictionService(
            CutoffRepository cutoffRepository,
            CategoryRepository categoryRepository,
            MLClient mlClient
    ) {
        this.cutoffRepository = cutoffRepository;
        this.categoryRepository = categoryRepository;
        this.mlClient = mlClient;
    }

    // ⭐ Risk based on probability (better than raw diff)
    private String riskFromProbability(double prob) {
        if (prob >= 0.8) return "SAFE";
        if (prob >= 0.5) return "MODERATE";
        return "RISKY";
    }

    // ⭐ Confidence from percentile gap
    private String confidenceFromGap(double gap) {
        double g = Math.abs(gap);

        if (g >= 10) return "HIGH";
        if (g >= 5) return "MEDIUM";
        return "LOW";
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

        // 1️⃣ Sort by closeness first
        List<Cutoff> sortedCutoffs =
                cutoffsPage.getContent().stream()
                        .sorted(Comparator.comparingDouble(
                                c -> Math.abs(
                                        request.getPercentile()
                                                - c.getCutoffPercentile()
                                )
                        ))
                        .toList();

        // 2️⃣ Collect cutoffs
        List<Double> cutoffValues =
                sortedCutoffs.stream()
                        .map(Cutoff::getCutoffPercentile)
                        .toList();

        // 3️⃣ ONE ML CALL
        List<Double> probabilities =
                mlClient.getBatchProbabilities(
                        request.getPercentile(),
                        cutoffValues
                );

        // 4️⃣ Build responses
        List<StudentPredictionResponse> responses = new ArrayList<>();

        for (int i = 0; i < sortedCutoffs.size(); i++) {

            Cutoff c = sortedCutoffs.get(i);

            double prob = probabilities.get(i);

            double gap =
                    request.getPercentile()
                            - c.getCutoffPercentile();

            responses.add(
                    new StudentPredictionResponse(
                            c.getCourse().getCollege().getCollegeName(),
                            c.getCourse().getCourseName(),
                            c.getCutoffPercentile(),
                            c.getRound(),
                            riskFromProbability(prob),
                            prob,
                            confidenceFromGap(gap)
                    )
            );
        }

        // ⭐ FINAL SORT by probability DESC (best first)
        responses.sort(
                Comparator.comparing(
                        StudentPredictionResponse::getProbability
                ).reversed()
        );

        return new PageImpl<>(
                responses,
                pageable,
                cutoffsPage.getTotalElements()
        );
    }
}
