package com.example.docudeep.api.dto;
import java.util.UUID;


public record DecisionDTO(UUID applicationId, double score, String outcome, String reason) {}
