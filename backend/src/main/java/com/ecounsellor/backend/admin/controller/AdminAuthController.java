package com.ecounsellor.backend.admin.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecounsellor.backend.admin.dto.LoginRequest;
import com.ecounsellor.backend.admin.entity.Admin;
import com.ecounsellor.backend.admin.repository.AdminRepository;
import com.ecounsellor.backend.admin.service.AdminAuthService;
import com.ecounsellor.backend.admin.service.AdminLogService;

/**
 * REPLACE the existing AdminAuthController.java with this file.
 *
 * Changes vs original:
 *  - Login response now includes { token, username, email } so the React
 *    frontend can display the admin's name in the topbar.
 *  - Added GET /api/admin/auth/me to refresh profile info.
 *  - Login writes an audit log entry.
 *
 * NOTE: The existing @RequestMapping("/auth") path is changed to
 *       "/api/admin/auth" so it sits under the /api/admin/** security rule.
 *       Update your frontend login URL accordingly:
 *         OLD: POST http://localhost:8080/auth/login
 *         NEW: POST http://localhost:8080/api/admin/auth/login
 *
 *       Also update SecurityConfig to permit the new path (done in the
 *       updated SecurityConfig.java provided alongside this file).
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService  authService;
    private final AdminRepository   adminRepo;
    private final AdminLogService   logService;

    public AdminAuthController(
            AdminAuthService authService,
            AdminRepository  adminRepo,
            AdminLogService  logService) {
        this.authService = authService;
        this.adminRepo   = adminRepo;
        this.logService  = logService;
    }

    /**
     * POST /api/admin/auth/login
     * Body: { "username": "admin", "password": "secret" }
     * Returns: { "token": "...", "username": "admin", "email": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());

            Admin admin = adminRepo.findByUsername(request.getUsername()).orElseThrow();

            logService.info(admin.getUsername(), "Admin login successful");

            return ResponseEntity.ok(Map.of(
                    "token",    token,
                    "username", admin.getUsername(),
                    "email",    admin.getEmail() != null ? admin.getEmail() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/auth/me
     * Returns the current admin's profile (no password).
     * Requires: Bearer token with ROLE_ADMIN
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return adminRepo.findByUsername(principal.getName())
                .map(a -> ResponseEntity.ok(Map.of(
                        "username", a.getUsername(),
                        "email",    a.getEmail() != null ? a.getEmail() : "",
                        "role",     a.getRole()  != null ? a.getRole()  : "ADMIN"
                )))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Admin not found")));
    }
}
