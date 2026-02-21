package com.ecounsellor.backend.college.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ecounsellor.backend.college.dto.CollegeDTO;
import com.ecounsellor.backend.college.dto.CollegeDTO.CourseDTO;
import com.ecounsellor.backend.core.entity.College;
import com.ecounsellor.backend.core.entity.Course;
import com.ecounsellor.backend.core.repository.CollegeRepository;
import com.ecounsellor.backend.core.repository.CourseRepository;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepo;
    private final CourseRepository courseRepo;

    public CollegeService(CollegeRepository collegeRepo, CourseRepository courseRepo) {
        this.collegeRepo = collegeRepo;
        this.courseRepo  = courseRepo;
    }

    // ── Get all districts (for frontend dropdown) ─────────────────────────────
    public List<String> getAllDistricts() {
        return collegeRepo.findAllDistricts();
    }

    // ── Get all regions ───────────────────────────────────────────────────────
    public List<String> getAllRegions() {
        return collegeRepo.findAllRegions();
    }

    // ── Get colleges by one or more districts (main feature) ─────────────────
    //    districts param is a list like ["Pune", "Nashik"]
    public List<CollegeDTO> getByDistricts(List<String> districts) {
        // Normalize to lowercase for query
        List<String> lowerDistricts = districts.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        List<College> colleges = collegeRepo.findByDistrictIn(lowerDistricts);

        if (colleges.isEmpty()) {
            return List.of();
        }

        // Batch-fetch all courses for these colleges in ONE query
        List<Long> collegeIds = colleges.stream()
                .map(College::getCollegeId)
                .collect(Collectors.toList());

        List<Course> allCourses = courseRepo.findByCollegeIds(collegeIds);

        // Group courses by collegeId
        Map<Long, List<Course>> coursesByCollege = allCourses.stream()
                .collect(Collectors.groupingBy(c -> c.getCollege().getCollegeId()));

        // Map to DTOs
        return colleges.stream()
                .map(college -> toDTO(college, coursesByCollege.getOrDefault(college.getCollegeId(), List.of())))
                .collect(Collectors.toList());
    }

    // ── Get all colleges (no filter) ──────────────────────────────────────────
    public List<CollegeDTO> getAll() {
        List<College> colleges = collegeRepo.findAll();

        List<Long> collegeIds = colleges.stream()
                .map(College::getCollegeId)
                .collect(Collectors.toList());

        List<Course> allCourses = courseRepo.findByCollegeIds(collegeIds);

        Map<Long, List<Course>> coursesByCollege = allCourses.stream()
                .collect(Collectors.groupingBy(c -> c.getCollege().getCollegeId()));

        return colleges.stream()
                .map(college -> toDTO(college, coursesByCollege.getOrDefault(college.getCollegeId(), List.of())))
                .collect(Collectors.toList());
    }

    // ── Get single college by ID ───────────────────────────────────────────────
    public CollegeDTO getById(Long id) {
        College college = collegeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("College not found with id: " + id));
        List<Course> courses = courseRepo.findByCollege_CollegeIdOrderByCourseName(id);
        return toDTO(college, courses);
    }

    // ── Get single college by code ─────────────────────────────────────────────
    public CollegeDTO getByCode(String collegeCode) {
        College college = collegeRepo.findByCollegeCode(collegeCode)
                .orElseThrow(() -> new RuntimeException("College not found with code: " + collegeCode));
        List<Course> courses = courseRepo.findByCollege_CollegeIdOrderByCourseName(college.getCollegeId());
        return toDTO(college, courses);
    }

    // ── Search by name ─────────────────────────────────────────────────────────
    public List<CollegeDTO> searchByName(String name) {
        List<College> colleges = collegeRepo.findByCollegeNameContainingIgnoreCase(name);

        List<Long> collegeIds = colleges.stream()
                .map(College::getCollegeId)
                .collect(Collectors.toList());

        List<Course> allCourses = collegeIds.isEmpty()
                ? List.of()
                : courseRepo.findByCollegeIds(collegeIds);

        Map<Long, List<Course>> coursesByCollege = allCourses.stream()
                .collect(Collectors.groupingBy(c -> c.getCollege().getCollegeId()));

        return colleges.stream()
                .map(college -> toDTO(college, coursesByCollege.getOrDefault(college.getCollegeId(), List.of())))
                .collect(Collectors.toList());
    }

    // ── Count ──────────────────────────────────────────────────────────────────
    public long count() {
        return collegeRepo.count();
    }

    // ── CREATE (admin only) ────────────────────────────────────────────────────
    public CollegeDTO create(CollegeDTO dto) {
        if (collegeRepo.findByCollegeCode(dto.getCollegeCode()).isPresent()) {
            throw new RuntimeException("College with code " + dto.getCollegeCode() + " already exists");
        }
        College college = new College();
        college.setCollegeCode(dto.getCollegeCode());
        college.setCollegeName(dto.getCollegeName());
        college.setCourseUniversity(dto.getCourseUniversity());
        college.setFundingType(dto.getFundingType());
        college.setIsAutonomous(dto.getIsAutonomous());
        college.setMinorityStatus(dto.getMinorityStatus());
        college.setTotalIntake(dto.getTotalIntake());
        college.setAddress(dto.getAddress());
        college.setRegion(dto.getRegion());
        college.setDistrict(dto.getDistrict());
        College saved = collegeRepo.save(college);
        return toDTO(saved, List.of());
    }

    // ── UPDATE (admin only) ────────────────────────────────────────────────────
    public CollegeDTO update(Long id, CollegeDTO dto) {
        College college = collegeRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("College not found with id: " + id));

        if (dto.getCollegeCode() != null && !dto.getCollegeCode().equals(college.getCollegeCode())) {
            if (collegeRepo.findByCollegeCode(dto.getCollegeCode()).isPresent()) {
                throw new RuntimeException("College with code " + dto.getCollegeCode() + " already exists");
            }
            college.setCollegeCode(dto.getCollegeCode());
        }
        if (dto.getCollegeName()     != null) college.setCollegeName(dto.getCollegeName());
        if (dto.getCourseUniversity()!= null) college.setCourseUniversity(dto.getCourseUniversity());
        if (dto.getFundingType()     != null) college.setFundingType(dto.getFundingType());
        if (dto.getIsAutonomous()    != null) college.setIsAutonomous(dto.getIsAutonomous());
        if (dto.getMinorityStatus()  != null) college.setMinorityStatus(dto.getMinorityStatus());
        if (dto.getTotalIntake()     != null) college.setTotalIntake(dto.getTotalIntake());
        if (dto.getAddress()         != null) college.setAddress(dto.getAddress());
        if (dto.getRegion()          != null) college.setRegion(dto.getRegion());
        if (dto.getDistrict()        != null) college.setDistrict(dto.getDistrict());

        College updated = collegeRepo.save(college);
        List<Course> courses = courseRepo.findByCollege_CollegeIdOrderByCourseName(updated.getCollegeId());
        return toDTO(updated, courses);
    }

    // ── DELETE (admin only) ────────────────────────────────────────────────────
    public void delete(Long id) {
        if (!collegeRepo.existsById(id)) {
            throw new RuntimeException("College not found with id: " + id);
        }
        collegeRepo.deleteById(id);
    }
    
    //*** ── Get all distinct course names (for branch dropdown) ───────────────────
    public List<String> getAllBranches() {
        return courseRepo.findAllDistinctCourseNames();
    }

    // ── Helper: College + Courses → DTO ───────────────────────────────────────
    private CollegeDTO toDTO(College college, List<Course> courses) {
        List<CourseDTO> courseDTOs = courses.stream()
                .map(c -> new CourseDTO(
                        c.getCourseId(),
                        c.getCourseCode(),
                        c.getCourseName(),
                        c.getCourseStatus(),
                        c.getIntake(),
                        c.getUniversity(),
                        c.getIsAutonomous(),
                        c.getMinorityStatus(),
                        c.getShift(),
                        c.getAccreditation(),
                        c.getGender()
                ))
                .collect(Collectors.toList());

        return new CollegeDTO(
                college.getCollegeId(),
                college.getCollegeCode(),
                college.getCollegeName(),
                college.getCourseUniversity(),
                college.getFundingType(),
                college.getIsAutonomous(),
                college.getMinorityStatus(),
                college.getTotalIntake(),
                college.getAddress(),
                college.getRegion(),
                college.getDistrict(),
                courseDTOs
        );
    }
}