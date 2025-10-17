package com.example.docudeep.api.dto;

import com.example.docudeep.DocumentType;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public record DocumentUploadPlanDTO(
        UUID documentId,
        DocumentType documentType,
        String uploadUrl,
        String method,
        Map<String, String> headers,
        Duration expiresIn
) {
}
