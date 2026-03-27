package com.ecounsellor.backend.admin.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecounsellor.backend.admin.dto.ImportPayload;
import com.ecounsellor.backend.admin.service.AdminImportService;
import com.ecounsellor.backend.admin.service.AdminLogService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/admin/import")
public class AdminImportController {

    private final AdminImportService importService;
    private final AdminLogService    logService;
    private final ObjectMapper       objectMapper;

    @Value("${cet.pipeline.dir:./cet_pipeline}")
    private String pipelineDir;

    @Value("${cet.pipeline.python:python}")
    private String pythonExe;

    public AdminImportController(
            AdminImportService importService,
            AdminLogService    logService,
            ObjectMapper       objectMapper) {
        this.importService = importService;
        this.logService    = logService;
        this.objectMapper  = objectMapper;
    }

    // ── POST /api/admin/import/push ───────────────────────────────────────────
    @PostMapping("/push")
    public ResponseEntity<?> push(
            @RequestBody ImportPayload payload,
            Principal principal) {
        try {
            String actor = principal != null ? principal.getName() : "admin";
            Map<String, Object> result = importService.pushBatch(
                payload.getRows(),
                payload.getYear() != null ? payload.getYear() : "unknown",
                actor
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/admin/import/scrape ─────────────────────────────────────────
    @PostMapping("/scrape")
    public ResponseEntity<?> scrape(
            @RequestBody Map<String, String> body,
            Principal principal) {

        String year   = body.getOrDefault("year",   "2024");
        String rounds = body.getOrDefault("rounds", "1,2,3,4");
        String actor  = principal != null ? principal.getName() : "admin";

        List<String> logLines = new ArrayList<>();

        try {
            // ── Resolve absolute paths ────────────────────────────────────────
            String absDir    = Paths.get(pipelineDir).toAbsolutePath().normalize().toString();
            String outputDir = Paths.get(absDir, "output").toString();
            String pipelineScript = Paths.get(absDir, "pipeline.py").toString();

            logLines.add("[pipeline] Dir: " + absDir);
            logLines.add("[pipeline] Python: " + pythonExe);
            logLines.add("[pipeline] Script: " + pipelineScript);

            // Verify files exist before running
            if (!new File(pipelineScript).exists()) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "pipeline.py not found at: " + pipelineScript,
                    "log",   logLines
                ));
            }

            // ── Run pipeline.py ───────────────────────────────────────────────
            logLines.add("[pipeline] Starting: year=" + year + " rounds=" + rounds);

            ProcessBuilder pb = new ProcessBuilder(
                pythonExe,
                pipelineScript,
                "--year",   year,
                "--rounds", rounds
            );
            pb.directory(new File(absDir));
            pb.redirectErrorStream(true);  // merge stderr+stdout

            Process process = pb.start();

            // Read ALL output
            StringBuilder fullOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                                          StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fullOutput.append(line).append("\n");
                    logLines.add(line);
                }
            }

            int exitCode = process.waitFor();
            logLines.add("[pipeline] Exit code: " + exitCode);

            // ── Find CLEAN_*.csv files ────────────────────────────────────────
            List<Path> cleanCsvs = new ArrayList<>();
            Path outPath = Paths.get(outputDir);

            if (!Files.exists(outPath)) {
                logLines.add("[pipeline] output dir does not exist: " + outputDir);
            } else {
                // List all files in output dir for debugging
                logLines.add("[pipeline] Files in output dir:");
                try (var stream = Files.list(outPath)) {
                    stream.sorted().forEach(p -> {
                        logLines.add("  " + p.getFileName().toString());
                        if (p.getFileName().toString().startsWith("CLEAN_")
                                && p.getFileName().toString().endsWith(".csv")) {
                            cleanCsvs.add(p);
                        }
                    });
                }
            }

            if (cleanCsvs.isEmpty()) {
                logService.warn(actor, "Pipeline ran but no CLEAN_*.csv found");
                return ResponseEntity.status(500).body(Map.of(
                    "error",      "Pipeline completed but no CLEAN_*.csv was produced.",
                    "pipelineLog", fullOutput.toString(),
                    "log",        logLines
                ));
            }

            logLines.add("[pipeline] Found " + cleanCsvs.size() + " CLEAN CSV(s)");

            // ── Parse CSV rows ────────────────────────────────────────────────
            List<Map<String, String>> allRows = new ArrayList<>();
            List<String> headers = new ArrayList<>();

            for (Path csvPath : cleanCsvs) {
                logLines.add("[pipeline] Parsing: " + csvPath.getFileName());
                List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
                if (lines.isEmpty()) continue;

                if (headers.isEmpty()) {
                    // Parse header — strip BOM and quotes
                    String headerLine = lines.get(0)
                        .replace("\uFEFF", "")
                        .replace("\"", "");
                    headers = Arrays.asList(headerLine.split(",", -1));
                }

                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;

                    // Handle quoted CSV values
                    String[] vals = line.replace("\"", "").split(",", -1);
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int j = 0; j < headers.size(); j++) {
                        row.put(headers.get(j).trim(),
                                j < vals.length ? vals[j].trim() : "");
                    }
                    allRows.add(row);
                }
            }

            logLines.add("[pipeline] Total rows: " + allRows.size());
            logService.success(actor,
                "Pipeline complete: year=" + year + " rows=" + allRows.size());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status",  "ok");
            response.put("rows",    allRows);
            response.put("headers", headers);
            response.put("source",  "scrape");
            response.put("year",    year);
            response.put("count",   allRows.size());
            response.put("log",     logLines);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logLines.add("EXCEPTION: " + e.getMessage());
            logService.error(actor, "Pipeline exception: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "log",   logLines
            ));
        }
    }

    // ── POST /api/admin/import/retrain ────────────────────────────────────────
    @PostMapping("/retrain")
    public ResponseEntity<?> retrain(Principal principal) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:8001/retrain"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"source\":\"db\"}"))
                .timeout(java.time.Duration.ofMinutes(5))
                .build();
            java.net.http.HttpResponse<String> resp =
                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String actor = principal != null ? principal.getName() : "admin";
            if (resp.statusCode() == 200) {
                logService.success(actor, "ML retrain triggered");
                return ResponseEntity.ok(resp.body());
            }
            return ResponseEntity.status(resp.statusCode())
                .body(Map.of("error", "ML returned " + resp.statusCode()));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                .body(Map.of("error", "ML unreachable: " + e.getMessage()));
        }
    }
}