package com.example.docudeep.api.dto;

import java.util.List;
import java.util.UUID;

public record CaseCreateResponse(
        UUID caseId,
        List<DocumentUploadPlanDTO> uploads
) {
}
