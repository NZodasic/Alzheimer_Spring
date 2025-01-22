package com.lashana.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imagePath;

    @Column(nullable = false)
    private String predictedLabel;

    @Column(nullable = false)
    private double confidence;

    private LocalDateTime createdAt = LocalDateTime.now();
}
