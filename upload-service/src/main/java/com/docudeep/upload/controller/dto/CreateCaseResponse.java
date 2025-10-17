package com.docudeep.upload.controller.dto;

import com.docudeep.upload.model.DocumentType;

import java.util.List;

public class CreateCaseResponse {

    private String caseId;
    private List<UploadSlot> uploads;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public List<UploadSlot> getUploads() {
        return uploads;
    }

    public void setUploads(List<UploadSlot> uploads) {
        this.uploads = uploads;
    }

    public static class UploadSlot {
        private String documentId;
        private String uploadUrl;
        private String method;
        private DocumentType documentType;

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public void setUploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public DocumentType getDocumentType() {
            return documentType;
        }

        public void setDocumentType(DocumentType documentType) {
            this.documentType = documentType;
        }
    }
}
