package com.ecounsellor.backend.student.repository;

import com.ecounsellor.backend.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByCetAppNumber(String cetAppNumber);

    // ── Admin panel queries ───────────────────────────────────────────────────

    /** Count of active students — used in Overview KPI */
    long countByActiveTrue();

    /** Count of suspended students */
    long countByActiveFalse();
}
