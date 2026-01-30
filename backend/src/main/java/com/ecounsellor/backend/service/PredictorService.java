package com.ecounsellor.backend.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecounsellor.backend.entity.Cutoff;
import com.ecounsellor.backend.repository.CutoffRepository;

@Service
public class PredictorService {

    @Autowired
    private CutoffRepository cutoffRepo;

    public List<Cutoff> predictColleges(Double percentile, String branch, String category) {
        return cutoffRepo.findByBranchAndCategoryAndRound(branch, category, 1)
                .stream()
                .filter(c -> percentile >= c.getPercentile())
                .sorted(Comparator.comparing(Cutoff::getPercentile).reversed())
                .toList();
    }
}
