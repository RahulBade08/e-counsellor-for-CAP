package com.ecounsellor.backend.core.repository;

import com.ecounsellor.backend.core.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {

    Optional<College> findByCollegeCode(String collegeCode);

    Optional<College> findByCollegeNameIgnoreCase(String collegeName);
}
