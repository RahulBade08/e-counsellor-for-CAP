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
 * Persists cleaned cutoff rows that come from the Data Import wizard.
 *
 * Strategy:
 *  - findOrCreate College by collegeCode
 *  - findOrCreate Course by (college, courseCode)
 *  - findOrCreate Category by categoryName
 *  - always INSERT a new Cutoff row (history is additive — we never update old records)
 *
 * All writes happen in a single transaction per batch call.
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

        // In-method caches to avoid repeated DB round-trips within the same batch
        Map<String, College>  collegeCache  = new HashMap<>();
        Map<String, Course>   courseCache   = new HashMap<>();
        Map<String, Category> categoryCache = new HashMap<>();

        int saved  = 0;
        int errors = 0;

        for (ImportRow row : rows) {
            try {
                // ── 1. College ────────────────────────────────────────────────
                String collegeKey = row.getCollege_code();
                College college = collegeCache.computeIfAbsent(collegeKey, code ->
                        collegeRepo.findByCollegeCode(code).orElseGet(() -> {
                            College c = new College();
                            c.setCollegeCode(code);
                            c.setCollegeName(row.getCollege_name() != null ? row.getCollege_name() : code);
                            return collegeRepo.save(c);
                        })
                );

                // ── 2. Course ─────────────────────────────────────────────────
                String courseKey = collegeKey + "|" + row.getCourse_code();
                Course course = courseCache.computeIfAbsent(courseKey, k ->
                        courseRepo.findByCourseCodeAndCollege_CollegeId(
                                row.getCourse_code(), college.getCollegeId())
                        .orElseGet(() -> {
                            Course c = new Course();
                            c.setCollege(college);
                            c.setCourseCode(row.getCourse_code());
                            c.setCourseName(row.getCourse_name() != null ? row.getCourse_name() : row.getCourse_code());
                            return courseRepo.save(c);
                        })
                );

                // ── 3. Category ───────────────────────────────────────────────
                String catKey = row.getCap_category();
                Category category = categoryCache.computeIfAbsent(catKey, name ->
                        categoryRepo.findByCategoryName(name).orElseGet(() -> {
                            Category cat = new Category();
                            cat.setCategoryName(name);
                            return categoryRepo.save(cat);
                        })
                );

                // ── 4. Cutoff row ─────────────────────────────────────────────
                Cutoff cutoff = new Cutoff();
                cutoff.setCourse(course);
                cutoff.setCategory(category);
                cutoff.setCapCategoryCode(row.getCap_category());
                cutoff.setGender(row.getGender());
                cutoff.setRound(row.getRound());
                cutoff.setLastRank(row.getLast_rank());
                cutoff.setCutoffPercentile(row.getCutoff_percentile());

                cutoffRepo.save(cutoff);
                saved++;

            } catch (Exception e) {
                errors++;
                // Continue processing remaining rows
            }
        }

        logService.success(adminUsername,
                "Imported cutoff data for year " + year + ": " + saved + " rows saved, " + errors + " errors");

        return Map.of("saved", saved, "errors", errors, "year", year);
    }
}
