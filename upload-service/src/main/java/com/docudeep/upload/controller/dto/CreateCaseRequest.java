package com.docudeep.upload.controller.dto;

import com.docudeep.upload.model.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateCaseRequest {

    @NotEmpty
    private List<@Valid DocumentSpecification> documents;

    public List<DocumentSpecification> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentSpecification> documents) {
        this.documents = documents;
    }

    public static class DocumentSpecification {

        @NotBlank
        private String filename;

        @NotBlank
        private String mimeType;

        @Positive
        private long sizeBytes;

        @NotNull
        private DocumentType documentType;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public DocumentType getDocumentType() {
            return documentType;
        }

        public void setDocumentType(DocumentType documentType) {
            this.documentType = documentType;
        }
    }
}
