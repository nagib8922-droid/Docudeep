package com.example.docudeep;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_folder")
@Data
public class CaseFolder {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private Instant createdAt = Instant.now();
    private String status = "OPEN";
}

