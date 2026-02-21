package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCodeAndCollege_CollegeId(String courseCode, Long collegeId);

    // ── All courses for a single college ─────────────────────────────────────
    List<Course> findByCollege_CollegeIdOrderByCourseName(Long collegeId);

    // ── All courses for a list of colleges (used in district filter) ─────────
    @Query("SELECT c FROM Course c WHERE c.college.collegeId IN :collegeIds ORDER BY c.college.collegeName, c.courseName")
    List<Course> findByCollegeIds(@Param("collegeIds") List<Long> collegeIds);
    
    // Returns all distinct course names sorted alphabetically.
    @Query("SELECT DISTINCT c.courseName FROM Course c WHERE c.courseName IS NOT NULL ORDER BY c.courseName")
    List<String> findAllDistinctCourseNames();

}