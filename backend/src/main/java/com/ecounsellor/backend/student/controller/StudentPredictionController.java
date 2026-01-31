package com.ecounsellor.backend.student.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.student.dto.StudentPredictionRequest;
import com.ecounsellor.backend.student.dto.StudentPredictionResponse;
import com.ecounsellor.backend.student.service.StudentPredictionService;


@RestController
@RequestMapping("/api/student")
public class StudentPredictionController {

    private final StudentPredictionService predictionService;

    public StudentPredictionController(StudentPredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/predict")
    public ResponseEntity<Page<StudentPredictionResponse>> predictColleges(
            @RequestBody StudentPredictionRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                predictionService.predictColleges(request, page, size)
        );
    }

}

