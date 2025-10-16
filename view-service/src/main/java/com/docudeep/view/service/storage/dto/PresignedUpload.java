package com.example.docudeep.service.storage.dto;

import java.time.Duration;
import java.util.Map;

public record PresignedUpload(String url, String method, Map<String, String> headers, Duration expiresIn) {
}
