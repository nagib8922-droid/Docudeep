package com.example.docudeep;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document")
@Data
public class Document {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CaseFolder caseFolder;

    private String filename;

    @Enumerated(EnumType.STRING)
    private DocumentType fileType;

    private String mimeType;
    private Long sizeBytes;
    private String storageKey;
    private String storageUrl;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PENDING_UPLOAD;

    private Instant createdAt;
    private Instant uploadedAt;

    @PrePersist
    public void onPersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = DocumentStatus.PENDING_UPLOAD;
        }
    }
}

