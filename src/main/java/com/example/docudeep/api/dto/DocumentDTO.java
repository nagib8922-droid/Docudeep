package com.example.docudeep.api.dto;

import com.example.docudeep.DocumentStatus;
import com.example.docudeep.DocumentType;

import java.util.UUID;

public record DocumentDTO(
        UUID id,
        DocumentType type,
        DocumentStatus status,
        String mimeType,
        Long sizeBytes,
        String storageUrl
) {
}
