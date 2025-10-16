package com.example.docudeep;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document")
@Data
public class Document {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private CaseFolder caseFolder;

    private String filename;
    private String fileType; // "PAYSLIP", "TAX_NOTICE", etc.
    private String mimeType;
    private Long sizeBytes;
    private String storageUrl;
    private Instant uploadedAt = Instant.now();
}

