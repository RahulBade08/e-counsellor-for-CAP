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

/**
 * Ordering logic follows the real-world MHT-CET / JoSAA counseling approach:
 *
 *  Zone 1 — SAFE      (prob >= 0.80): sorted by cutoff DESC
 *            Best reachable colleges where admission is almost certain.
 *            Higher cutoff = more competitive/prestigious college → shown first.
 *
 *  Zone 2 — MODERATE  (prob 0.50–0.79): sorted by probability DESC, then cutoff DESC
 *            Colleges with a reasonable chance. Highest probability shown first.
 *
 *  Zone 3 — RISKY     (prob < 0.50): sorted by probability DESC
 *            Stretch/aspirational colleges — lowest probability last.
 *
 * KEY FIX: Sorting now happens on the FULL deduped list BEFORE pagination.
 * The old code sorted only the current page slice, causing random ordering
 * across pages (e.g. 88.4, 72.2, 40.9, 79.4 appearing mixed on the same page).
 */
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

    // ── Zone & label helpers ───────────────────────────────────────────────────

    /** Returns 1=SAFE, 2=MODERATE, 3=RISKY for primary sort key. */
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

    // ── Main prediction method ─────────────────────────────────────────────────

    public Page<StudentPredictionResponse> predictColleges(
            StudentPredictionRequest request,
            int page,
            int size) {

        Integer round  = request.getRound() != null ? request.getRound() : 4;
        String capCode = request.derivedCapCategoryCode();

        List<String> branches  = request.getBranchesLower();
        List<String> districts = request.getDistrictsLower();
        boolean hasBranch      = !branches.isEmpty();
        boolean hasDistrict    = !districts.isEmpty();

        // Fetch all eligible rows (we sort in memory — DB order doesn't matter here)
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

        // Dedup: one entry per college+course combination
        Map<String, Cutoff> best = new LinkedHashMap<>();
        for (Cutoff c : cutoffsPage.getContent()) {
            String key = c.getCourse().getCollege().getCollegeId()
                       + "_" + c.getCourse().getCourseId();
            best.putIfAbsent(key, c);
        }

        List<Cutoff> deduped      = new ArrayList<>(best.values());
        int          totalDeduped = deduped.size();

        // ── ML probabilities for the ENTIRE deduped list ──────────────────────
        // Must compute probabilities before sorting — we sort BY probability.
        List<Double> allCutoffValues = deduped.stream()
                .map(Cutoff::getCutoffPercentile).toList();

        List<Double> allProbabilities = mlClient.getBatchProbabilities(
                request.getPercentile(), allCutoffValues);

        // ── Build full response list with probabilities attached ──────────────
        List<StudentPredictionResponse> allResponses = new ArrayList<>();
        for (int i = 0; i < deduped.size(); i++) {
            Cutoff  c       = deduped.get(i);
            Course  course  = c.getCourse();
            College college = course.getCollege();

            double prob = allProbabilities.get(i);
            double gap  = request.getPercentile() - c.getCutoffPercentile();

            allResponses.add(new StudentPredictionResponse(
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

        // ── Sort FULL list before paginating (3-zone MHT-CET / JoSAA style) ──
        allResponses.sort(Comparator
                // Primary: zone (1=SAFE first, 3=RISKY last)
                .<StudentPredictionResponse>comparingInt(r -> zone(r.getProbability()))
                // Secondary: within-zone ordering
                .thenComparing((a, b) -> {
                    int zA = zone(a.getProbability());
                    if (zA == 1) {
                        // SAFE zone: higher cutoff = more prestigious college first
                        return Double.compare(b.getCutoffPercentile(), a.getCutoffPercentile());
                    } else {
                        // MODERATE/RISKY: higher probability first
                        int probCmp = Double.compare(b.getProbability(), a.getProbability());
                        if (probCmp != 0) return probCmp;
                        // Tiebreak: higher cutoff first
                        return Double.compare(b.getCutoffPercentile(), a.getCutoffPercentile());
                    }
                })
        );

        // ── Paginate the now-correctly-sorted full list ───────────────────────
        int from      = page * size;
        int to        = Math.min(from + size, totalDeduped);
        List<StudentPredictionResponse> pageSlice = from >= totalDeduped
                ? List.of() : allResponses.subList(from, to);

        return new PageImpl<>(new ArrayList<>(pageSlice), PageRequest.of(page, size), totalDeduped);
    }
}