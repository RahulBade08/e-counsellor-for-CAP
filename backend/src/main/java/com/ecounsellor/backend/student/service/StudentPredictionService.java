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
    private final MLClient mlClient;

    public StudentPredictionService(CutoffRepository cutoffRepository, MLClient mlClient) {
        this.cutoffRepository = cutoffRepository;
        this.mlClient = mlClient;
    }

    private int zone(double prob) {
        if (prob >= 0.80) return 1;
        if (prob >= 0.50) return 2;
        return 3;
    }

    private String riskLabel(double prob) {
        if (prob >= 0.80) return "SAFE";
        if (prob >= 0.50) return "MODERATE";
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

        Integer round  = request.getRound() != null ? request.getRound() : 4;
        String capCode = request.derivedCapCategoryCode();

        // expandedBranches() converts group labels -> exact DB course names
        List<String> branches  = request.expandedBranches();
        // districtListLower() lowercases for LOWER(col.district) SQL match
        List<String> districts = request.districtListLower();

        boolean hasBranch   = !branches.isEmpty();
        boolean hasDistrict = !districts.isEmpty();

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

        // Dedup: one row per college + course
        Map<String, Cutoff> best = new LinkedHashMap<>();
        for (Cutoff c : cutoffsPage.getContent()) {
            String key = c.getCourse().getCollege().getCollegeId()
                    + "_" + c.getCourse().getCourseId();
            best.putIfAbsent(key, c);
        }

        List<Cutoff> deduped = new ArrayList<>(best.values());
        int total = deduped.size();

        if (total == 0) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        // ML probabilities for full list — must happen before sorting
        List<Double> cutoffValues = deduped.stream()
                .map(Cutoff::getCutoffPercentile).toList();
        List<Double> probs = mlClient.getBatchProbabilities(
                request.getPercentile(), cutoffValues);

        // Build response list
        List<StudentPredictionResponse> responses = new ArrayList<>();
        for (int i = 0; i < deduped.size(); i++) {
            Cutoff  c       = deduped.get(i);
            Course  course  = c.getCourse();
            College college = course.getCollege();
            double  prob    = probs.get(i);
            double  gap     = request.getPercentile() - c.getCutoffPercentile();

            responses.add(new StudentPredictionResponse(
                    college.getCollegeName(),
                    college.getCollegeCode(),
                    course.getCourseName(),
                    c.getCutoffPercentile(),
                    c.getRound(),
                    riskLabel(prob),
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

        // Sort full list before paginating
        // Zone 1 SAFE (>=0.80):     higher cutoff first
        // Zone 2 MODERATE (>=0.50): higher probability first
        // Zone 3 RISKY (<0.50):     higher probability first
        responses.sort(Comparator
                .<StudentPredictionResponse>comparingInt(r -> zone(r.getProbability()))
                .thenComparing((a, b) -> {
                    int zA = zone(a.getProbability());
                    if (zA == 1) {
                        return Double.compare(b.getCutoffPercentile(), a.getCutoffPercentile());
                    } else {
                        int cmp = Double.compare(b.getProbability(), a.getProbability());
                        return cmp != 0 ? cmp
                                : Double.compare(b.getCutoffPercentile(), a.getCutoffPercentile());
                    }
                })
        );

        int from = page * size;
        int to   = Math.min(from + size, total);
        List<StudentPredictionResponse> slice = from >= total
                ? List.of() : responses.subList(from, to);

        return new PageImpl<>(new ArrayList<>(slice), PageRequest.of(page, size), total);
    }
}