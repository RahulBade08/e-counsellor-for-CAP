package com.ecounsellor.backend.counselling.service;

import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.repository.CollegeRepository;
import com.ecounsellor.backend.core.repository.CutoffRepository;
import com.ecounsellor.backend.counselling.dto.CounsellingDTOs.*;
import com.ecounsellor.backend.counselling.entity.StudentShortlist;
import com.ecounsellor.backend.counselling.entity.StudentView;
import com.ecounsellor.backend.counselling.repository.StudentShortlistRepository;
import com.ecounsellor.backend.counselling.repository.StudentViewRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CounsellingService {

    private final StudentViewRepository      viewRepo;
    private final StudentShortlistRepository shortlistRepo;
    private final CutoffRepository           cutoffRepo;
    private final CollegeRepository          collegeRepo;

    public CounsellingService(
            StudentViewRepository      viewRepo,
            StudentShortlistRepository shortlistRepo,
            CutoffRepository           cutoffRepo,
            CollegeRepository          collegeRepo) {
        this.viewRepo      = viewRepo;
        this.shortlistRepo = shortlistRepo;
        this.cutoffRepo    = cutoffRepo;
        this.collegeRepo   = collegeRepo;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EVENT TRACKING
    // ══════════════════════════════════════════════════════════════════════════

    public void recordView(ViewEventRequest req) {
        StudentView v = new StudentView();
        v.setCollegeCode(req.collegeCode);
        v.setCourseCode(req.courseCode);
        v.setStudentPercentile(req.studentPercentile);
        v.setCategory(req.category);
        v.setGender(req.gender);
        v.setAdmissionType(req.admissionType);
        viewRepo.save(v);
    }

    public void recordShortlist(ShortlistRequest req) {
        StudentShortlist s = new StudentShortlist();
        s.setCollegeCode(req.collegeCode);
        s.setCourseCode(req.courseCode != null ? req.courseCode : "");
        s.setCourseName(req.courseName);
        s.setStudentPercentile(req.studentPercentile);
        s.setCategory(req.category);
        s.setGender(req.gender);
        s.setAdmissionType(req.admissionType);
        s.setCapCategoryCode(req.capCategoryCode);
        shortlistRepo.save(s);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 1: INTERESTED STUDENTS
    // ══════════════════════════════════════════════════════════════════════════

    public InterestedStudentsResponse getInterestedStudents(String collegeCode) {
        InterestedStudentsResponse resp = new InterestedStudentsResponse();
        resp.collegeCode     = collegeCode;
        resp.totalViews      = viewRepo.countByCollegeCode(collegeCode);
        resp.totalShortlists = shortlistRepo.countByCollegeCode(collegeCode);

        // Percentile band distribution from views
        List<Object[]> bandRows = viewRepo.percentileBandDistribution(collegeCode);
        resp.percentileBands = bandRows.stream()
            .map(r -> new PercentileBand((String) r[0], (Long) r[1]))
            .collect(Collectors.toList());

        // Category breakdown from views
        List<Object[]> catRows = viewRepo.countViewsByCategory(collegeCode, null);
        resp.byCategory = catRows.stream()
            .map(r -> new CategoryCount((String) r[0], (Long) r[1]))
            .collect(Collectors.toList());

        // Branch-level shortlist counts
        List<Object[]> branchRows    = shortlistRepo.countShortlistsByBranch(collegeCode);
        List<Object[]> branchCatRows = shortlistRepo.countShortlistsByBranchAndCategory(collegeCode);

        // Group categories by courseCode
        Map<String, List<CategoryCount>> catByBranch = new LinkedHashMap<>();
        for (Object[] r : branchCatRows) {
            String code = (String) r[0];
            catByBranch.computeIfAbsent(code, k -> new ArrayList<>())
                       .add(new CategoryCount((String) r[2], (Long) r[3]));
        }

        // View counts per branch
        Map<String, Long> viewsByBranch = new LinkedHashMap<>();
        for (Object[] r : viewRepo.countViewsByCourse(collegeCode)) {
            viewsByBranch.put((String) r[0], (Long) r[1]);
        }

        resp.byBranch = branchRows.stream().map(r -> {
            String code       = (String) r[0];
            String name       = (String) r[1];
            long   shortlists = (Long)   r[2];
            long   views      = viewsByBranch.getOrDefault(code, 0L);
            return new BranchInterest(code, name, shortlists, views,
                catByBranch.getOrDefault(code, List.of()));
        }).collect(Collectors.toList());

        return resp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 2: TARGET POOL
    // ══════════════════════════════════════════════════════════════════════════

    public TargetPoolResponse getTargetPool(
            String collegeCode, String courseCode,
            String capCategoryCode, int round) {

        List<Double> cutoffs = cutoffRepo.findCutoffForPrediction(
            collegeCode, courseCode, capCategoryCode);

        double lastCutoff      = cutoffs.isEmpty() ? 50.0 : cutoffs.get(0);
        double predictedCutoff = lastCutoff + (round > 2 ? 0.5 : 1.0);
        double targetMin       = Math.max(0,   Math.round((predictedCutoff - 5.0)  * 100.0) / 100.0);
        double targetMax       = Math.min(100, Math.round((predictedCutoff + 10.0) * 100.0) / 100.0);
        String category        = extractCategory(capCategoryCode);

        long eligible = viewRepo.countPotentialTargets(collegeCode, targetMin, targetMax, category);
        long already  = shortlistRepo.countExistingShortlists(
            collegeCode, courseCode, targetMin, targetMax, category);

        TargetPoolResponse resp = new TargetPoolResponse();
        resp.collegeCode            = collegeCode;
        resp.courseCode             = courseCode;
        resp.capCategoryCode        = capCategoryCode;
        resp.targetMin              = targetMin;
        resp.targetMax              = targetMax;
        resp.estimatedEligibleInApp = eligible;
        resp.alreadyShortlistedUs   = already;
        resp.notYetAware            = Math.max(0, eligible - already);
        resp.note = String.format(
            "Last round cutoff: %.1f. Predicted next: %.1f. " +
            "Target range %.1f–%.1f covers MODERATE to SAFE zone.",
            lastCutoff, predictedCutoff, targetMin, targetMax);
        return resp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 3: TARGET RANGES
    // ══════════════════════════════════════════════════════════════════════════

    public TargetRangesResponse getTargetRanges(String collegeCode, int round) {
        College college = collegeRepo.findByCollegeCode(collegeCode)
            .orElseThrow(() -> new RuntimeException("College not found: " + collegeCode));

        List<Object[]> rows = cutoffRepo.findLatestCutoffsByCollegeAndRound(collegeCode, round);

        // Deduplicate by branch+category+gender
        Map<String, Object[]> dedupMap = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String key = r[0] + "_" + r[2] + "_" + r[3];
            dedupMap.putIfAbsent(key, r);
        }

        List<BranchTargetRange> branches = new ArrayList<>();
        for (Object[] r : dedupMap.values()) {
            String courseCode      = (String) r[0];
            String courseName      = (String) r[1];
            String capCatCode      = (String) r[2];
            String gender          = (String) r[3];
            Double lastCutoff      = r[4] != null ? (Double) r[4] : 50.0;
            int    intake          = r[5] != null ? ((Number) r[5]).intValue() : 0;

            double predictedCutoff = lastCutoff + (round < 3 ? 1.5 : 0.5);
            double targetMin       = Math.max(0,   Math.round((predictedCutoff - 5.0)  * 10.0) / 10.0);
            double targetMax       = Math.min(100, Math.round((predictedCutoff + 10.0) * 10.0) / 10.0);
            String trend           = predictedCutoff > lastCutoff + 0.5 ? "RISING"
                                   : predictedCutoff < lastCutoff - 0.5 ? "FALLING" : "STABLE";
            String category        = extractCategory(capCatCode);

            long   already  = shortlistRepo.countExistingShortlists(
                                collegeCode, courseCode, targetMin, targetMax, category);
            Double avgPct   = shortlistRepo.avgPercentileForBranch(collegeCode, courseCode);

            BranchTargetRange btr = new BranchTargetRange();
            btr.courseCode              = courseCode;
            btr.courseName              = courseName;
            btr.capCategoryCode         = capCatCode;
            btr.category                = category;
            btr.gender                  = gender;
            btr.intake                  = intake;
            btr.lastRoundCutoff         = lastCutoff;
            btr.predictedCutoff         = Math.round(predictedCutoff * 100.0) / 100.0;
            btr.predictionConfidence    = round == 4 ? "HIGH" : round >= 2 ? "MEDIUM" : "LOW";
            btr.targetMin               = targetMin;
            btr.targetMax               = targetMax;
            btr.alreadyShortlisted      = already;
            btr.avgInterestedPercentile = avgPct != null ? Math.round(avgPct * 10.0) / 10.0 : null;
            btr.rationale = String.format(
                "Last cutoff: %.1f. Predicted: %.1f (%s). Target %.1f–%.1f.",
                lastCutoff, btr.predictedCutoff, trend, targetMin, targetMax);
            branches.add(btr);
        }

        TargetRangesResponse resp = new TargetRangesResponse();
        resp.collegeCode = collegeCode;
        resp.collegeName = college.getCollegeName();
        resp.round       = round;
        resp.branches    = branches;
        return resp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 4: CUTOFF HISTORY — FIXED (broken nested computeIfAbsent removed)
    // ══════════════════════════════════════════════════════════════════════════

    public CutoffHistoryResponse getCutoffHistory(String collegeCode) {
        College college = collegeRepo.findByCollegeCode(collegeCode)
            .orElseThrow(() -> new RuntimeException("College not found: " + collegeCode));

        List<Object[]> rows = cutoffRepo.findCutoffHistoryByCollegeCode(collegeCode);

        // branchMap: courseCode → BranchCutoffHistory
        Map<String, BranchCutoffHistory> branchMap = new LinkedHashMap<>();
        // catMap: courseCode → catKey → CategoryCutoffHistory
        Map<String, Map<String, CategoryCutoffHistory>> catMap = new LinkedHashMap<>();

        for (Object[] r : rows) {
            String courseCode = (String)  r[0];
            String courseName = (String)  r[1];
            String capCatCode = (String)  r[2];
            String gender     = (String)  r[3];
            int    round      = ((Number) r[4]).intValue();
            Double cutoff     = (Double)  r[5];
            int    intake     = r[6] != null ? ((Number) r[6]).intValue() : 0;

            // Ensure branch entry
            branchMap.computeIfAbsent(courseCode,
                k -> new BranchCutoffHistory(courseCode, courseName, intake));

            // Ensure inner map
            catMap.computeIfAbsent(courseCode, k -> new LinkedHashMap<>());

            // Ensure category entry — FIXED: no nested lambda
            String catKey = capCatCode + "_" + gender;
            Map<String, CategoryCutoffHistory> innerMap = catMap.get(courseCode);
            if (!innerMap.containsKey(catKey)) {
                CategoryCutoffHistory cat = new CategoryCutoffHistory(capCatCode, gender);
                cat.roundHistory = new ArrayList<>();
                innerMap.put(catKey, cat);
            }
            innerMap.get(catKey).roundHistory.add(new RoundCutoff(round, cutoff));
        }

        // Compute predictions and trends
        List<BranchCutoffHistory> branchList = new ArrayList<>();
        for (Map.Entry<String, BranchCutoffHistory> entry : branchMap.entrySet()) {
            String              courseCode = entry.getKey();
            BranchCutoffHistory branch     = entry.getValue();
            Map<String, CategoryCutoffHistory> cats =
                catMap.getOrDefault(courseCode, Collections.emptyMap());

            List<CategoryCutoffHistory> catList = new ArrayList<>();
            for (CategoryCutoffHistory cat : cats.values()) {
                cat.roundHistory.sort(Comparator.comparingInt(rc -> rc.round));
                if (!cat.roundHistory.isEmpty()) {
                    double last  = cat.roundHistory.get(cat.roundHistory.size() - 1).cutoffPercentile != null
                                 ? cat.roundHistory.get(cat.roundHistory.size() - 1).cutoffPercentile : 50.0;
                    double first = cat.roundHistory.get(0).cutoffPercentile != null
                                 ? cat.roundHistory.get(0).cutoffPercentile : last;
                    double trend = last - first;
                    double inc   = cat.roundHistory.size() > 1
                                 ? Math.min(3.0, Math.max(-2.0, trend / cat.roundHistory.size())) : 0.5;
                    cat.predictedNextCutoff = Math.round((last + inc) * 100.0) / 100.0;
                    cat.trend = inc > 0.3 ? "RISING" : inc < -0.3 ? "FALLING" : "STABLE";
                }
                catList.add(cat);
            }
            branch.byCategory = catList;
            branchList.add(branch);
        }

        CutoffHistoryResponse resp = new CutoffHistoryResponse();
        resp.collegeCode = collegeCode;
        resp.collegeName = college.getCollegeName();
        resp.branches    = branchList;
        return resp;
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String extractCategory(String capCode) {
        if (capCode == null) return null;
        if ("EWS".equals(capCode) || "TFWS".equals(capCode)) return capCode;
        String mid = capCode.replaceAll("^[GL]", "").replaceAll("[HSO]$", "");
        return switch (mid) {
            case "OPEN"      -> "OPEN";
            case "OBC"       -> "OBC";
            case "SC"        -> "SC";
            case "ST"        -> "ST";
            case "NT1", "VJ" -> "NT1";
            case "NT2"       -> "NT2";
            case "NT3"       -> "NT3";
            default          -> mid;
        };
    }
}