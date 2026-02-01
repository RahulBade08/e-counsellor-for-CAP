package com.ecounsellor.backend.core.ml;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class MLClient {

    private final RestTemplate restTemplate;

    public MLClient() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(1000); // 1s
        factory.setReadTimeout(1500);    // 1.5s

        this.restTemplate = new RestTemplate(factory);
    }

    public List<Double> getBatchProbabilities(
            double studentPercentile,
            List<Double> cutoffs
    ) {

        try {
            String url = "http://localhost:8001/predict-batch";

            Map<String, Object> request = Map.of(
                    "student_percentile", studentPercentile,
                    "cutoff_percentiles", cutoffs
            );

            Map response =
                    restTemplate.postForObject(url, request, Map.class);

            return (List<Double>) response.get("probabilities");

        } catch (Exception e) {

            System.out.println("⚠️ ML service unavailable, using fallback");

            // fallback probability
            return cutoffs.stream()
                    .map(c -> fallbackProbability(
                            studentPercentile,
                            c
                    ))
                    .toList();
        }
    }

    private double fallbackProbability(
            double student,
            double cutoff
    ) {
        double gap = student - cutoff;

        if (gap >= 1.5) return 0.95;
        if (gap >= 1.0) return 0.85;
        if (gap >= 0.5) return 0.70;
        if (gap >= 0.2) return 0.60;
        if (gap >= 0.1) return 0.55;
        return 0.50;
    }

}
