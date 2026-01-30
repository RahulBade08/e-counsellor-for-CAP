package com.ecounsellor.backend.student.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.student.entity.Cutoff;
import com.ecounsellor.backend.student.service.PredictorService;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private PredictorService predictorService;

    @GetMapping("/predict")
    public List<Cutoff> predict(
        @RequestParam Double percentile,
        @RequestParam String branch,
        @RequestParam String category
    ) {
        return predictorService.predictColleges(percentile, branch, category);
    }
}
