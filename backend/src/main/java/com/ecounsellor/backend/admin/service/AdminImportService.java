package com.ecounsellor.backend.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecounsellor.backend.admin.dto.ImportRow;
import com.ecounsellor.backend.core.entity.Category;
import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.entity.Course;
import com.ecounsellor.backend.core.entity.Cutoff;
import com.ecounsellor.backend.core.repository.CategoryRepository;
import com.ecounsellor.backend.core.repository.CollegeRepository;
import com.ecounsellor.backend.core.repository.CourseRepository;
import com.ecounsellor.backend.core.repository.CutoffRepository;

/**
 * Persists cleaned cutoff rows from the Admin Panel Data Import wizard.
 *
 * Key fixes over original:
 *   1. Uses row.resolvedCategory()    — works with both extractor.py and generic CSV
 *   2. Uses row.getSafeCutoffPercentile() — guards against negative values
 *   3. Sets regional_reservation and last_cap_round on Cutoff entity
 *   4. Sets course_status and university on Course when creating new courses
 *   5. Gender field removed entirely
 */
@Service
public class AdminImportService {

    private final CollegeRepository  collegeRepo;
    private final CourseRepository   courseRepo;
    private final CutoffRepository   cutoffRepo;
    private final CategoryRepository categoryRepo;
    private final AdminLogService    logService;

    public AdminImportService(
            CollegeRepository  collegeRepo,
            CourseRepository   courseRepo,
            CutoffRepository   cutoffRepo,
            CategoryRepository categoryRepo,
            AdminLogService    logService) {
        this.collegeRepo  = collegeRepo;
        this.courseRepo   = courseRepo;
        this.cutoffRepo   = cutoffRepo;
        this.categoryRepo = categoryRepo;
        this.logService   = logService;
    }

    @Transactional
    public Map<String, Object> pushBatch(List<ImportRow> rows, String year, String adminUsername) {

        // In-batch caches — avoid repeated DB hits within the same batch call
        Map<String, College>  collegeCache  = new HashMap<>();
        Map<String, Course>   courseCache   = new HashMap<>();
        Map<String, Category> categoryCache = new HashMap<>();

        int saved  = 0;
        int errors = 0;

        for (ImportRow row : rows) {
            try {
                // ── Validate minimum required fields ──────────────────────────
                if (row.getCollege_code() == null || row.getCollege_code().isBlank()) continue;
                if (row.getCourse_code()  == null || row.getCourse_code().isBlank())  continue;
                if (row.getSafeCutoffPercentile() == null)                            continue;

                // ── 1. College — findOrCreate ─────────────────────────────────
                String collegeKey = row.getCollege_code().strip();
                College college = collegeCache.computeIfAbsent(collegeKey, code ->
                    collegeRepo.findByCollegeCode(code).orElseGet(() -> {
                        College c = new College();
                        c.setCollegeCode(code);
                        c.setCollegeName(
                            row.getCollege_name() != null && !row.getCollege_name().isBlank()
                                ? row.getCollege_name().strip()
                                : code
                        );
                        return collegeRepo.save(c);
                    })
                );

                // ── 2. Course — findOrCreate ──────────────────────────────────
                String courseKey = collegeKey + "|" + row.getCourse_code().strip();
                Course course = courseCache.computeIfAbsent(courseKey, k ->
                    courseRepo.findByCourseCodeAndCollege_CollegeId(
                            row.getCourse_code().strip(), college.getCollegeId())
                        .orElseGet(() -> {
                            Course c = new Course();
                            c.setCollege(college);
                            c.setCourseCode(row.getCourse_code().strip());
                            c.setCourseName(
                                row.getCourse_name() != null && !row.getCourse_name().isBlank()
                                    ? row.getCourse_name().strip()
                                    : row.getCourse_code().strip()
                            );
                            // Additional fields from extractor.py
                            if (row.getCourse_status() != null && !row.getCourse_status().isBlank())
                                c.setCourseStatus(row.getCourse_status().strip());
                            if (row.getCourse_university() != null && !row.getCourse_university().isBlank())
                                c.setUniversity(row.getCourse_university().strip());
                            return courseRepo.save(c);
                        })
                );

                // ── 3. Category — findOrCreate ────────────────────────────────
                String catName = row.resolvedCategory();   // handles both sources
                Category category = categoryCache.computeIfAbsent(catName, name ->
                    categoryRepo.findByCategoryName(name).orElseGet(() -> {
                        Category cat = new Category();
                        cat.setCategoryName(name);
                        return categoryRepo.save(cat);
                    })
                );

                // ── 4. Cutoff — always insert new row (history is additive) ───
                Cutoff cutoff = new Cutoff();
                cutoff.setCourse(course);
                cutoff.setCategory(category);
                cutoff.setCapCategoryCode(catName);

                // Safe (always positive) percentile — fixes the negative bug
                cutoff.setCutoffPercentile(row.getSafeCutoffPercentile());

                cutoff.setLastRank(row.getLast_rank() != null
                    ? Math.abs(row.getLast_rank())   // guard against negative ranks too
                    : null);

                // Fields from extractor.py
                if (row.getRegional_reservation() != null && !row.getRegional_reservation().isBlank())
                    cutoff.setRegionalReservation(row.getRegional_reservation().strip());

                if (row.getLast_cap_round() != null)
                    cutoff.setLastCapRound(row.getLast_cap_round());

                cutoffRepo.save(cutoff);
                saved++;

            } catch (Exception e) {
                errors++;
                // Continue with next row — don't fail the whole batch
            }
        }

        logService.success(adminUsername,
            "Imported cutoff data for year " + year
            + ": " + saved + " rows saved, " + errors + " errors");

        return Map.of("saved", saved, "errors", errors, "year", year);
    }
}