package com.ecounsellor.backend.student.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.entity.Course;
import com.ecounsellor.backend.core.entity.Cutoff;
import com.ecounsellor.backend.core.ml.MLClient;
import com.ecounsellor.backend.core.repository.CutoffRepository;
import com.ecounsellor.backend.student.dto.StudentPredictionRequest;
import com.ecounsellor.backend.student.dto.StudentPredictionResponse;

@Service
public class StudentPredictionService {

    private final CutoffRepository cutoffRepository;
    private final MLClient         mlClient;

    public StudentPredictionService(
            CutoffRepository cutoffRepository,
            MLClient mlClient) {
        this.cutoffRepository = cutoffRepository;
        this.mlClient         = mlClient;
    }

    private String riskFromProbability(double prob) {
        if (prob >= 0.8) return "SAFE";
        if (prob >= 0.5) return "MODERATE";
        return "RISKY";
    }

    private String confidenceFromGap(double gap) {
        double g = Math.abs(gap);
        if (g >= 10) return "HIGH";
        if (g >= 5)  return "MEDIUM";
        return "LOW";
    }

    public Page<StudentPredictionResponse> predictColleges(
            StudentPredictionRequest request,
            int page,
            int size) {

        Integer round   = request.getRound() != null ? request.getRound() : 4;

        // Derive exact cap_category_code — one unique code, no ambiguity
        String capCode  = request.derivedCapCategoryCode();

        List<String> branches  = request.getBranchesLower();
        List<String> districts = request.getDistrictsLower();
        boolean hasBranch      = !branches.isEmpty();
        boolean hasDistrict    = !districts.isEmpty();

        // Fetch all rows for this exact cap code (no pagination yet — dedup first)
        Pageable all = PageRequest.of(0, 10_000);

        Page<Cutoff> cutoffsPage;
        if (hasBranch && hasDistrict) {
            cutoffsPage = cutoffRepository.findEligibleByBranchesAndDistricts(
                    capCode, round, request.getPercentile(), branches, districts, all);
        } else if (hasBranch) {
            cutoffsPage = cutoffRepository.findEligibleByBranches(
                    capCode, round, request.getPercentile(), branches, all);
        } else if (hasDistrict) {
            cutoffsPage = cutoffRepository.findEligibleByDistricts(
                    capCode, round, request.getPercentile(), districts, all);
        } else {
            cutoffsPage = cutoffRepository.findEligible(
                    capCode, round, request.getPercentile(), all);
        }

        // ── Dedup: one exact capCode per college+course → should be 1 row now ──
        // putIfAbsent still guards against any edge-case duplicates
        Map<String, Cutoff> best = new LinkedHashMap<>();
        for (Cutoff c : cutoffsPage.getContent()) {
            String key = c.getCourse().getCollege().getCollegeId()
                       + "_" + c.getCourse().getCourseId();
            best.putIfAbsent(key, c);
        }

        List<Cutoff> deduped     = new ArrayList<>(best.values());
        int          totalDeduped = deduped.size();

        // ── Manual pagination on deduped list ────────────────────────────────
        int from      = page * size;
        int to        = Math.min(from + size, totalDeduped);
        List<Cutoff> pageSlice = from >= totalDeduped
                ? List.of() : deduped.subList(from, to);

        // ── Batch ML call ─────────────────────────────────────────────────────
        List<Double> cutoffValues = pageSlice.stream()
                .map(Cutoff::getCutoffPercentile).toList();

        List<Double> probabilities = mlClient.getBatchProbabilities(
                request.getPercentile(), cutoffValues);

        // ── Build responses ───────────────────────────────────────────────────
        List<StudentPredictionResponse> responses = new ArrayList<>();

        for (int i = 0; i < pageSlice.size(); i++) {
            Cutoff  c       = pageSlice.get(i);
            Course  course  = c.getCourse();
            College college = course.getCollege();

            double prob = probabilities.get(i);
            double gap  = request.getPercentile() - c.getCutoffPercentile();

            responses.add(new StudentPredictionResponse(
                    college.getCollegeName(),
                    college.getCollegeCode(),
                    course.getCourseName(),
                    c.getCutoffPercentile(),
                    c.getRound(),
                    riskFromProbability(prob),
                    prob,
                    confidenceFromGap(gap),
                    college.getDistrict(),
                    college.getRegion(),
                    college.getAddress(),
                    college.getFundingType(),
                    college.getIsAutonomous(),
                    course.getIntake()
            ));
        }

        // Best probability first
        responses.sort(Comparator.comparing(
                StudentPredictionResponse::getProbability).reversed());

        return new PageImpl<>(responses, PageRequest.of(page, size), totalDeduped);
    }
}