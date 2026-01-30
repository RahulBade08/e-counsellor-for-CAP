package com.ecounsellor.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecounsellor.backend.entity.College;

public interface CollegeRepository extends JpaRepository<College, String> {
	
}
