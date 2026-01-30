package com.ecounsellor.backend.college.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.college.entity.College;
import com.ecounsellor.backend.college.repository.CollegeRepository;

@RestController
@RequestMapping("/api/colleges")
public class CollegeController {

    @Autowired
    private CollegeRepository repo;

    @GetMapping
    public List<College> getAll() {
        return repo.findAll();
    }
}