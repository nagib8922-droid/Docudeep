package com.example.docudeep.api.dto;

import java.util.UUID;

public record ApplicationDTO(UUID id, UUID applicantId, double amount, String status) {
}
