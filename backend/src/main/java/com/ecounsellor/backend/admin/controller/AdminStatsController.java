package com.ecounsellor.backend.admin.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.admin.service.AdminLogService;
import com.ecounsellor.backend.admin.service.AdminStatsService;

/**
 * Endpoints for the Overview and System Logs pages of the Admin Dashboard.
 *
 * GET /api/admin/stats  — KPI counts (colleges, students, pending, cutoffs …)
 * GET /api/admin/test   — Health check (replaces the one in AdminController)
 * GET /api/admin/logs   — Recent 200 audit log entries
 */
@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final AdminStatsService statsService;
    private final AdminLogService   logService;

    public AdminStatsController(AdminStatsService statsService, AdminLogService logService) {
        this.statsService = statsService;
        this.logService   = logService;
    }

    
    /** System-wide KPI counts for the Overview dashboard */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    /** Recent admin activity log — used by the System Logs page */
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs() {
        return ResponseEntity.ok(logService.getRecent());
    }
}
