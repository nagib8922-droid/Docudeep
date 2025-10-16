package com.example.docudeep.service.storage;

import com.example.docudeep.service.storage.dto.PresignedUpload;

public interface StorageService {

    PresignedUpload prepareUpload(String storageKey, String mimeType, long contentLength);

    byte[] load(String storageKey);

    String resolveUrl(String storageKey);
}
