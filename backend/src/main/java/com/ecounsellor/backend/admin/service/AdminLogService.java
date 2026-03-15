package com.ecounsellor.backend.admin.service;

import com.ecounsellor.backend.admin.entity.AdminLog;
import com.ecounsellor.backend.admin.repository.AdminLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper around AdminLogRepository.
 * Inject this into any controller/service that needs to write an audit log entry.
 *
 * Usage:
 *   logService.log("admin", "Approved college 06155", "success");
 *   logService.log("system", "ML retrain complete. Accuracy: 0.87", "info");
 */
@Service
public class AdminLogService {

    private final AdminLogRepository repo;

    public AdminLogService(AdminLogRepository repo) {
        this.repo = repo;
    }

    // ── Write a log entry ─────────────────────────────────────────────────────

    public void log(String actor, String message, String type) {
        AdminLog entry = new AdminLog();
        entry.setActor(actor);
        entry.setMessage(message);
        entry.setType(type);
        repo.save(entry);
    }

    // Shorthand variants

    public void info(String actor, String message)    { log(actor, message, "info"); }
    public void success(String actor, String message) { log(actor, message, "success"); }
    public void warn(String actor, String message)    { log(actor, message, "warning"); }
    public void error(String actor, String message)   { log(actor, message, "error"); }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<AdminLog> getRecent() {
        return repo.findTop200ByOrderByCreatedAtDesc();
    }
}
