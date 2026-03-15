package com.ecounsellor.backend.admin.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.admin.dto.ImportPayload;
import com.ecounsellor.backend.admin.service.AdminImportService;
import com.ecounsellor.backend.admin.service.AdminLogService;

/**
 * Handles cutoff data import from the Data Import wizard.
 *
 * POST /api/admin/import/push
 *   Body: { "year": "2024", "rows": [ { cleaned ImportRow objects } ] }
 *   Writes rows to the database via AdminImportService.
 *
 * POST /api/admin/import/retrain
 *   Triggers the ML Python service to retrain with the latest DB data.
 *   The ML service must expose POST http://localhost:8001/retrain.
 *
 * NOTE: Selenium scraping is done on the Python/ML side, not here.
 *       If you want the backend to trigger a scrape, add a separate
 *       endpoint that calls the Python scraper service via HTTP.
 */
@RestController
@RequestMapping("/api/admin/import")
public class AdminImportController {

    private final AdminImportService importService;
    private final AdminLogService    logService;

    public AdminImportController(
            AdminImportService importService,
            AdminLogService    logService) {
        this.importService = importService;
        this.logService    = logService;
    }

    /**
     * Accepts the cleaned rows from the React frontend and writes them to the DB.
     * Called in batches of ~100 rows by the frontend.
     */
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

    /**
     * Proxy endpoint: tells the ML Python service to retrain using the latest DB data.
     * The React ML page calls this instead of hitting the ML service directly,
     * so the admin token is validated by Spring Security first.
     *
     * The ML service must expose:  POST http://localhost:8001/retrain
     */
    @PostMapping("/retrain")
    public ResponseEntity<?> retrain(Principal principal) {
        try {
            // Call the Python ML service
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8001/retrain"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"source\":\"db\"}"))
                    .timeout(java.time.Duration.ofMinutes(5))   // retrain can take a while
                    .build();

            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            String actor = principal != null ? principal.getName() : "admin";
            if (response.statusCode() == 200) {
                logService.success(actor, "ML model retrain triggered successfully");
                return ResponseEntity.ok(response.body());
            } else {
                logService.error(actor, "ML retrain failed — HTTP " + response.statusCode());
                return ResponseEntity.status(response.statusCode())
                        .body(Map.of("error", "ML service returned " + response.statusCode()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "ML service unreachable: " + e.getMessage()));
        }
    }
}
