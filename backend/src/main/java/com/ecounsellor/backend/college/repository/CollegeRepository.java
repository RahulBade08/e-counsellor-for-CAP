package com.ecounsellor.backend.college.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecounsellor.backend.college.entity.College;

public interface CollegeRepository extends JpaRepository<College, String> {
	
}
