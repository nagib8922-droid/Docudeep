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
    private UUID id;

    private Instant createdAt;
    private String status = "OPEN";

    @PrePersist
    public void onPersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "OPEN";
        }
    }
}

