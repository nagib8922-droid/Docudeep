package com.example.docudeep.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CaseCreateRequest(
        @NotEmpty @Size(max = 5) List<@Valid DocumentUploadRequest> documents
) {
}
