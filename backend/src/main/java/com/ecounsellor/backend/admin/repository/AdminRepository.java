package com.ecounsellor.backend.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecounsellor.backend.admin.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {

Optional<Admin> findByUsername(String username);
}

