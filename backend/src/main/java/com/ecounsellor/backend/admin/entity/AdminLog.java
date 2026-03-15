package com.ecounsellor.backend.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persists every admin action so the System Logs page has real data.
 * TABLE: admin_logs
 */
@Entity
@Table(name = "admin_logs")
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "admin" | "system" | username of the admin who acted */
    @Column(name = "actor", length = 100)
    private String actor;

    /** Human-readable description of the action */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** "success" | "error" | "warning" | "info" — maps to badge colour in UI */
    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                      { return id; }

    public String getActor()                 { return actor; }
    public void setActor(String actor)       { this.actor = actor; }

    public String getMessage()               { return message; }
    public void setMessage(String message)   { this.message = message; }

    public String getType()                  { return type; }
    public void setType(String type)         { this.type = type; }

    public LocalDateTime getCreatedAt()      { return createdAt; }
}
