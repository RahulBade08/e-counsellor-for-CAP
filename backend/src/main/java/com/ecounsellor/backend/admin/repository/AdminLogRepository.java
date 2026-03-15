package com.ecounsellor.backend.admin.repository;

import com.ecounsellor.backend.admin.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    /** Most recent 200 log entries — used by the Logs page */
    List<AdminLog> findTop200ByOrderByCreatedAtDesc();
}
