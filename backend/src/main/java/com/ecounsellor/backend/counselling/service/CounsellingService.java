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

    // FIX (Bug 3 — double insert): CollegeResultAdapter.sendShortlistEvent() fires
    // POST /api/counselling/event/shortlist for anonymous tracking.
    // StudentAuthService.addShortlist() also writes to student_shortlists for
    // logged-in students via POST /api/student/me/shortlist.
    // If both fire for the same action the count is doubled.
    //
    // Solution: recordShortlist() (anonymous endpoint) now checks whether a
    // matching row already exists for this college+courseName+percentile+category
    // within the last 30 seconds before inserting. This is a lightweight guard
    // that prevents the duplicate without requiring a unique DB constraint
    // (which would break legitimate repeat shortlists from different students
    // who happen to share the same percentile bucket).
    public void recordShortlist(ShortlistRequest req) {
        // Normalise courseCode/courseName — adapter sends name in both fields
        String resolvedName = (req.courseName != null && !req.courseName.isBlank())
                ? req.courseName
                : req.courseCode;

        // Duplicate guard: skip if an identical row was inserted in the last 60 s.
        // This covers the race between the anonymous tracking call and the
        // StudentAuthService call for logged-in students.
        boolean recentDuplicate = shortlistRepo
            .existsRecentDuplicate(
                req.collegeCode,
                resolvedName,
                req.studentPercentile,
                req.category);

        if (recentDuplicate) return;

        StudentShortlist s = new StudentShortlist();
        s.setCollegeCode(req.collegeCode);
        // Always persist the human-readable name in courseCode so grouping works
        s.setCourseCode(resolvedName != null ? resolvedName : "");
        s.setCourseName(resolvedName);
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
        // FIX (Bug 1): countShortlistsByBranch now groups by courseName only,
        // so Civil Engineering stored with two different courseCode values
        // collapses into one row correctly.
        List<Object[]> branchRows    = shortlistRepo.countShortlistsByBranch(collegeCode);
        List<Object[]> branchCatRows = shortlistRepo.countShortlistsByBranchAndCategory(collegeCode);

        // Group categories by courseName (index 0 in fixed query = courseName)
        Map<String, List<CategoryCount>> catByBranch = new LinkedHashMap<>();
        for (Object[] r : branchCatRows) {
            String name = (String) r[0]; // courseName is now in position 0
            catByBranch.computeIfAbsent(name, k -> new ArrayList<>())
                       .add(new CategoryCount((String) r[2], (Long) r[3]));
        }

        // View counts per branch — views use courseCode, shortlists use courseName.
        // We join on courseCode from views to courseName in shortlists by trying
        // both the raw courseCode and the stored courseName.
        Map<String, Long> viewsByBranch = new LinkedHashMap<>();
        for (Object[] r : viewRepo.countViewsByCourse(collegeCode)) {
            viewsByBranch.put((String) r[0], (Long) r[1]);
        }

        resp.byBranch = branchRows.stream().map(r -> {
            // r[0] = courseName (used as display key), r[1] = courseName, r[2] = count
            String name       = (String) r[0];
            long   shortlists = (Long)   r[2];
            // Try to find views by matching the stored courseName to courseCode in views
            long views = viewsByBranch.getOrDefault(name, 0L);
            return new BranchInterest(name, name, shortlists, views,
                catByBranch.getOrDefault(name, List.of()));
        }).collect(Collectors.toList());

        return resp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FEATURE 2: TARGET POOL
    // ══════════════════════════════════════════════════════════════════════════

    public TargetPoolResponse getTargetPool(
            String collegeCode, String courseCode,
            String capCategoryCode, int round) {

        // capCategoryCode from frontend is a real CAP code like "GONT1S", "GOPENH"
        // We need the courseName stored in student_shortlists — but the college
        // admin types a courseCode ("101", "CE"). We look it up from cutoffs.
        String resolvedCourseName = resolveCourseName(collegeCode, courseCode);

        List<Double> cutoffs = cutoffRepo.findCutoffForPrediction(
            collegeCode, courseCode, capCategoryCode);

        double lastCutoff      = cutoffs.isEmpty() ? 50.0 : cutoffs.get(0);
        double predictedCutoff = lastCutoff + (round > 2 ? 0.5 : 1.0);
        double targetMin       = Math.max(0,   Math.round((predictedCutoff - 5.0)  * 100.0) / 100.0);
        double targetMax       = Math.min(100, Math.round((predictedCutoff + 10.0) * 100.0) / 100.0);

        // FIX (Bug 2a): extractCategory() now correctly maps all CAP codes
        String category = extractCategory(capCategoryCode);

        // FIX (Bug 2b): countPotentialTargets now counts DISTINCT student
        // fingerprints across the app, not views of other colleges
        long eligible = viewRepo.countPotentialTargets(collegeCode, targetMin, targetMax, category);

        // FIX (Bug 2c): pass resolvedCourseName (human-readable) not raw courseCode
        // so it matches what is stored in student_shortlists.courseName
        long already  = shortlistRepo.countExistingShortlists(
            collegeCode, resolvedCourseName != null ? resolvedCourseName : courseCode,
            targetMin, targetMax, category);

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

            // FIX (Bug 2a): use fixed extractCategory
            String category = extractCategory(capCatCode);

            // FIX (Bug 2c): match by courseName, not courseCode
            long   already  = shortlistRepo.countExistingShortlists(
                                collegeCode, courseName, targetMin, targetMax, category);
            Double avgPct   = shortlistRepo.avgPercentileForBranch(collegeCode, courseName);

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
    // FEATURE 4: CUTOFF HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    public CutoffHistoryResponse getCutoffHistory(String collegeCode) {
        College college = collegeRepo.findByCollegeCode(collegeCode)
            .orElseThrow(() -> new RuntimeException("College not found: " + collegeCode));

        List<Object[]> rows = cutoffRepo.findCutoffHistoryByCollegeCode(collegeCode);

        Map<String, BranchCutoffHistory> branchMap = new LinkedHashMap<>();
        Map<String, Map<String, CategoryCutoffHistory>> catMap = new LinkedHashMap<>();

        for (Object[] r : rows) {
            String courseCode = (String)  r[0];
            String courseName = (String)  r[1];
            String capCatCode = (String)  r[2];
            String gender     = (String)  r[3];
            int    round      = ((Number) r[4]).intValue();
            Double cutoff     = (Double)  r[5];
            int    intake     = r[6] != null ? ((Number) r[6]).intValue() : 0;

            branchMap.computeIfAbsent(courseCode,
                k -> new BranchCutoffHistory(courseCode, courseName, intake));

            catMap.computeIfAbsent(courseCode, k -> new LinkedHashMap<>());

            String catKey = capCatCode + "_" + gender;
            Map<String, CategoryCutoffHistory> innerMap = catMap.get(courseCode);
            if (!innerMap.containsKey(catKey)) {
                CategoryCutoffHistory cat = new CategoryCutoffHistory(capCatCode, gender);
                cat.roundHistory = new ArrayList<>();
                innerMap.put(catKey, cat);
            }
            innerMap.get(catKey).roundHistory.add(new RoundCutoff(round, cutoff));
        }

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

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * FIX (Bug 2a) — extractCategory: corrected mapping for all MHT-CET CAP codes.
     *
     * Real Maharashtra CAP category code format:
     *   [G|L] + CATEGORY + [S|H|O]
     *   prefix G = General/State, L = Ladies / Home University
     *   suffix S = State, H = Home University, O = Other
     *
     * Special standalone codes: EWS, TFWS (no prefix/suffix)
     *
     * Previous bugs:
     *  1. VJ was merged into NT1 — VJ (Vimukta Jati) is a distinct category (NT1 / VJ-A).
     *     Maharashtra CAP code is GOVJS. extractCategory should return "VJ" to match the
     *     category value stored by students who selected "VJ" in the app.
     *  2. LOPEN stripped to "OPEN" correctly, but the mid-string regex
     *     replaceAll("^[GL]","").replaceAll("[HSO]$","") would turn "LOPENH" into "OPEN"
     *     but "LOPEN" (missing suffix) into "OPEN" as well — that's fine. However the
     *     old code mapped "NT1" and "VJ" to the same output which is wrong.
     *  3. The CAP_CODES list in the frontend included "LOPEN" which is not a real code.
     *     The real ladies/open code is "LOPENS" (State quota). We keep the backend
     *     tolerant and just normalise the prefix/suffix stripping.
     */
    private String extractCategory(String capCode) {
        if (capCode == null) return null;
        // Standalone special codes
        if ("EWS".equals(capCode))  return "EWS";
        if ("TFWS".equals(capCode)) return "TFWS";

        // Strip leading G or L prefix, then strip trailing S, H, or O suffix
        String mid = capCode
            .replaceAll("^[GL]", "")
            .replaceAll("[SHO]$", "");

        return switch (mid.toUpperCase()) {
            case "OPEN"        -> "OPEN";
            case "OBC", "OBC1" -> "OBC";
            case "SC"          -> "SC";
            case "ST"          -> "ST";
            // VJ (Vimukta Jati) is distinct from NT1. Maharashtra lists VJ separately.
            // Students who select "VJ" in the app have category="VJ".
            case "VJ"          -> "VJ";
            case "NT1"         -> "NT1";
            case "NT2"         -> "NT2";
            case "NT3"         -> "NT3";
            case "NT"          -> "NT1";  // some older data uses bare "NT"
            case "SBC"         -> "OBC";  // Special Backward Class maps to OBC bucket
            default            -> mid;    // pass through unknown codes unchanged
        };
    }

    /**
     * Look up the human-readable course name for a given college + courseCode.
     * Used by getTargetPool() so the correct courseName is passed to
     * countExistingShortlists() (which matches on courseName, not courseCode).
     * Returns null if no cutoff data found — caller falls back to raw courseCode.
     */
    private String resolveCourseName(String collegeCode, String courseCode) {
        try {
            List<Object[]> rows = cutoffRepo.findLatestCutoffsByCollegeAndRound(collegeCode, 4);
            for (Object[] r : rows) {
                if (courseCode.equalsIgnoreCase((String) r[0])) {
                    return (String) r[1]; // courseName at index 1
                }
            }
            // Try round 3 if round 4 has no data
            rows = cutoffRepo.findLatestCutoffsByCollegeAndRound(collegeCode, 3);
            for (Object[] r : rows) {
                if (courseCode.equalsIgnoreCase((String) r[0])) {
                    return (String) r[1];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}