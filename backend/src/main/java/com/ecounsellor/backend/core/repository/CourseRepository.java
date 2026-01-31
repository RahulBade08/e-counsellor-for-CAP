package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.Course;
import com.ecounsellor.backend.core.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCollege(College college);

    Optional<Course> findByCourseCodeAndCollege(String courseCode, College college);
}
