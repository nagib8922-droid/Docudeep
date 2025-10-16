package com.example.docudeep.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DocumentUploadRequest(
        @NotBlank String filename,
        @NotBlank String mimeType,
        @NotNull @Positive Long sizeBytes,
        @NotBlank String documentType
) {
}
