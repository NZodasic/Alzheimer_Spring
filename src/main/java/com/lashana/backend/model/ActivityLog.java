package com.lashana.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String action; // Ví dụ: "UPLOAD_IMAGE", "DELETE_IMAGE", "LOGIN"

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
