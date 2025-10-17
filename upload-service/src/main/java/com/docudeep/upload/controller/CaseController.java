package com.docudeep.upload.controller;

import com.docudeep.upload.controller.dto.CreateCaseRequest;
import com.docudeep.upload.controller.dto.CreateCaseResponse;
import com.docudeep.upload.controller.dto.DocumentResponse;
import com.docudeep.upload.model.DocumentMetadata;
import com.docudeep.upload.service.CaseStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping
public class CaseController {

    private static final long TEN_MB = 10L * 1024 * 1024;

    private final CaseStorageService storageService;

    public CaseController(CaseStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(path = "/cases", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CreateCaseResponse createCase(@Valid @RequestBody CreateCaseRequest request) throws IOException {
        if (request.getDocuments().size() > 5) {
            throw new IllegalArgumentException("Un dossier ne peut pas contenir plus de 5 documents.");
        }

        request.getDocuments().forEach(doc -> {
            if (doc.getSizeBytes() > TEN_MB) {
                throw new IllegalArgumentException("La taille maximale par document est de 10 Mo.");
            }
            if (!isSupportedMimeType(doc.getMimeType())) {
                throw new IllegalArgumentException("Format de fichier non supporté : " + doc.getMimeType());
            }
        });

        List<CaseStorageService.DocumentRequest> specs = request.getDocuments().stream()
            .map(doc -> new CaseStorageService.DocumentRequest(doc.getFilename(), doc.getMimeType(), doc.getSizeBytes(), doc.getDocumentType()))
            .toList();

        var metadata = storageService.createCase(specs);

        String uploadUrlTemplate = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/storage/cases/{caseId}/documents/{documentId}")
            .toUriString();

        CreateCaseResponse response = new CreateCaseResponse();
        response.setCaseId(metadata.getCaseId());
        response.setUploads(metadata.getDocuments().stream().map(doc -> {
            CreateCaseResponse.UploadSlot slot = new CreateCaseResponse.UploadSlot();
            slot.setDocumentId(doc.getId());
            slot.setDocumentType(doc.getDocumentType());
            slot.setMethod("PUT");
            slot.setUploadUrl(uploadUrlTemplate
                .replace("{caseId}", metadata.getCaseId())
                .replace("{documentId}", doc.getId()));
            return slot;
        }).toList());
        return response;
    }

    @PutMapping(path = "/storage/cases/{caseId}/documents/{documentId}", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> uploadDocument(@PathVariable String caseId,
                                               @PathVariable String documentId,
                                               HttpServletRequest request) throws IOException {
        storageService.storeDocument(caseId, documentId, request.getInputStream());
        return ResponseEntity.accepted().build();
    }

    @PostMapping(path = "/cases/{caseId}/documents/{documentId}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentResponse completeDocument(@PathVariable String caseId, @PathVariable String documentId) throws IOException {
        DocumentMetadata document = storageService.validateDocument(caseId, documentId);
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setFilename(document.getFilename());
        response.setDocumentType(document.getDocumentType());
        response.setStatus(document.getStatus());
        response.setUploadedAt(document.getUploadedAt());
        response.setSizeBytes(document.getStoredSize());
        return response;
    }

    private boolean isSupportedMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return false;
        }
        return mimeType.equals("application/pdf") ||
            mimeType.equals("image/png") ||
            mimeType.equals("image/jpeg");
    }
}
