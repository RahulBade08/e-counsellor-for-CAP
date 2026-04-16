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
    //
    // FIX: The old response returned {rows, headers, source, year} which the
    // frontend ignored (it expected {files: [{filename, round, year}]}).
    //
    // New response includes BOTH:
    //   - files: [{filename, round, year}]  — shown in the "Downloaded PDFs" table
    //   - rows/headers/source/year          — used to populate rawData → step 2
    //
    // The frontend's handleScrape now sets rawData from the response and
    // advances to step 2 automatically when rows are present.
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
            String absDir         = Paths.get(pipelineDir).toAbsolutePath().normalize().toString();
            String outputDir      = Paths.get(absDir, "output").toString();
            String pipelineScript = Paths.get(absDir, "pipeline.py").toString();

            logLines.add("[pipeline] Dir: "    + absDir);
            logLines.add("[pipeline] Python: " + pythonExe);
            logLines.add("[pipeline] Script: " + pipelineScript);

            if (!new File(pipelineScript).exists()) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "pipeline.py not found at: " + pipelineScript,
                    "log",   logLines
                ));
            }

            logLines.add("[pipeline] Starting: year=" + year + " rounds=" + rounds);

            ProcessBuilder pb = new ProcessBuilder(
                pythonExe, pipelineScript,
                "--year",   year,
                "--rounds", rounds
            );
            pb.directory(new File(absDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder fullOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fullOutput.append(line).append("\n");
                    logLines.add(line);
                }
            }

            int exitCode = process.waitFor();
            logLines.add("[pipeline] Exit code: " + exitCode);

            // ── Scan output dir for CLEAN_*.csv and PDF files ─────────────────
            List<Map<String, String>> fileEntries = new ArrayList<>();   // for frontend "files" table
            List<Path> cleanCsvs = new ArrayList<>();
            Path outPath = Paths.get(outputDir);

            if (!Files.exists(outPath)) {
                logLines.add("[pipeline] output dir does not exist: " + outputDir);
            } else {
                logLines.add("[pipeline] Files in output dir:");
                try (var stream = Files.list(outPath)) {
                    stream.sorted().forEach(p -> {
                        String fname = p.getFileName().toString();
                        logLines.add("  " + fname);

                        // Collect CLEAN CSVs for data parsing
                        if (fname.startsWith("CLEAN_") && fname.endsWith(".csv")) {
                            cleanCsvs.add(p);
                        }

                        // ── Build file entry for frontend "Downloaded PDFs" table
                        // Filenames from cet_scraper.py follow pattern:
                        //   MeritList_<year>_Round<N>.pdf  OR  CLEAN_<year>_Round<N>.csv
                        // Extract round number from filename.
                        if (fname.endsWith(".pdf") || fname.endsWith(".csv")) {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("filename", fname);
                            entry.put("year", year);
                            // Parse round from filename e.g. "Round1", "round_1", "_R1_"
                            String roundNum = extractRound(fname);
                            entry.put("round", roundNum);
                            fileEntries.add(entry);
                        }
                    });
                }
            }

            if (cleanCsvs.isEmpty()) {
                logService.warn(actor, "Pipeline ran but no CLEAN_*.csv found");
                return ResponseEntity.status(500).body(Map.of(
                    "error",       "Pipeline completed but no CLEAN_*.csv was produced.",
                    "pipelineLog", fullOutput.toString(),
                    "files",       fileEntries,
                    "log",         logLines
                ));
            }

            logLines.add("[pipeline] Found " + cleanCsvs.size() + " CLEAN CSV(s)");

            // ── Parse CSV rows (robust quoted-field parser) ───────────────────
            List<Map<String, String>> allRows = new ArrayList<>();
            List<String> headers = new ArrayList<>();

            for (Path csvPath : cleanCsvs) {
                logLines.add("[pipeline] Parsing: " + csvPath.getFileName());
                List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
                if (lines.isEmpty()) continue;

                if (headers.isEmpty()) {
                    String headerLine = lines.get(0).replace("\uFEFF", "");
                    headers = parseCsvLine(headerLine);
                }

                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty()) continue;

                    List<String> vals = parseCsvLine(line);
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int j = 0; j < headers.size(); j++) {
                        row.put(headers.get(j).trim(), j < vals.size() ? vals.get(j).trim() : "");
                    }
                    allRows.add(row);
                }
            }

            logLines.add("[pipeline] Total rows: " + allRows.size());
            logService.success(actor,
                "Pipeline complete: year=" + year + " rows=" + allRows.size());

            // ── Build response that satisfies BOTH frontend requirements ──────
            // 1. files[]          → "Downloaded PDFs" table in Scrape step
            // 2. rows/headers     → rawData to advance to Preview step
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status",  "ok");
            response.put("files",   fileEntries);   // ← FIX: was missing before
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

    /**
     * Robust CSV line parser that correctly handles quoted fields containing commas.
     * e.g.  `"College of Engg, Pune",06002,OPEN`  →  ["College of Engg, Pune", "06002", "OPEN"]
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // Handle escaped double-quotes ("")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * Extracts round number from a filename.
     * Handles patterns: Round1, round_1, _R1_, R01, round-2, etc.
     * Returns "?" if not found.
     */
    private String extractRound(String filename) {
        // Try patterns: Round1, Round_1, Round-1, R1 (case-insensitive)
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?i)[Rr]ound[_\\-]?(\\d+)|[_\\-]R(\\d+)[_\\-.]")
            .matcher(filename);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return "?";
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