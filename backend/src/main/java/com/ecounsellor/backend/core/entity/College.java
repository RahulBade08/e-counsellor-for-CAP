package com.ecounsellor.backend.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "colleges")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collegeId;

    @Column(name = "college_code", nullable = false, unique = true)
    private String collegeCode;

    @Column(name = "college_name", nullable = false)
    private String collegeName;

    @Column(name = "course_university")
    private String courseUniversity;
}
