package com.docudeep.view.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class DocumentMetadata {

    private String id;
    private String filename;
    private String storageFilename;
    private String mimeType;
    private String documentType;
    private String status;
    private long storedSize;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant uploadedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStorageFilename() {
        return storageFilename;
    }

    public void setStorageFilename(String storageFilename) {
        this.storageFilename = storageFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStoredSize() {
        return storedSize;
    }

    public void setStoredSize(long storedSize) {
        this.storedSize = storedSize;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
