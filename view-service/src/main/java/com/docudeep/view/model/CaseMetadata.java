package com.docudeep.view.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CaseMetadata {

    private String caseId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;
    private List<DocumentMetadata> documents = new ArrayList<>();

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<DocumentMetadata> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentMetadata> documents) {
        this.documents = documents;
    }
}
