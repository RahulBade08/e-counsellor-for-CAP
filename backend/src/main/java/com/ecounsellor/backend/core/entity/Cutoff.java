package com.ecounsellor.backend.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cutoffs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Cutoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cutoffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "cap_category_code")
    private String capCategoryCode;

    @Column(name = "gender")
    private String gender;

    @Column(name = "regional_reservation")
    private String regionalReservation;

    @Column(name = "round")
    private Integer round;

    @Column(name = "last_cap_round")
    private Integer lastCapRound;

    @Column(name = "last_rank")
    private Integer lastRank;

    @Column(name = "cutoff_percentile")
    private Double cutoffPercentile;
}
