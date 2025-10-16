package com.example.docudeep.service;

import com.example.docudeep.CaseFolder;
import com.example.docudeep.Document;
import com.example.docudeep.DocumentStatus;
import com.example.docudeep.DocumentType;
import com.example.docudeep.api.dto.CaseCreateRequest;
import com.example.docudeep.api.dto.CaseCreateResponse;
import com.example.docudeep.api.dto.DocumentDTO;
import com.example.docudeep.api.dto.DocumentUploadPlanDTO;
import com.example.docudeep.api.dto.DocumentUploadRequest;
import com.example.docudeep.repo.CaseFolderRepository;
import com.example.docudeep.repo.DocumentRepository;
import com.example.docudeep.service.storage.StorageService;
import com.example.docudeep.service.storage.dto.PresignedUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseService {

    private static final long TEN_MB = 10 * 1024 * 1024L;
    private static final List<String> SUPPORTED_MIME_TYPES = List.of("application/pdf", "image/png", "image/jpeg");

    private final CaseFolderRepository caseFolderRepository;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final DocumentValidationService documentValidationService;

    @Transactional
    public CaseCreateResponse createCase(CaseCreateRequest request) {
        List<DocumentUploadRequest> documents = request.documents();
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("Au moins un document doit être fourni");
        }
        if (documents.size() > 5) {
            throw new IllegalArgumentException("Vous pouvez uploader au maximum 5 documents");
        }

        CaseFolder caseFolder = new CaseFolder();
        caseFolder = caseFolderRepository.save(caseFolder);

        List<DocumentUploadPlanDTO> uploads = new ArrayList<>();

        for (DocumentUploadRequest docRequest : documents) {
            validateMetadata(docRequest);

            Document document = new Document();
            document.setCaseFolder(caseFolder);
            document.setFilename(docRequest.filename());
            document.setFileType(DocumentType.fromLabel(docRequest.documentType()));
            document.setMimeType(docRequest.mimeType());
            document.setSizeBytes(docRequest.sizeBytes());

            document = documentRepository.save(document);

            String storageKey = buildStorageKey(caseFolder.getId(), document.getId(), docRequest.filename());
            document.setStorageKey(storageKey);
            document.setStorageUrl(storageService.resolveUrl(storageKey));

            document = documentRepository.save(document);

            PresignedUpload presignedUpload = storageService.prepareUpload(storageKey, document.getMimeType(), document.getSizeBytes());

            DocumentUploadPlanDTO uploadPlan = new DocumentUploadPlanDTO(
                    document.getId(),
                    document.getFileType(),
                    presignedUpload.url(),
                    presignedUpload.method(),
                    presignedUpload.headers(),
                    presignedUpload.expiresIn()
            );
            uploads.add(uploadPlan);
        }

        return new CaseCreateResponse(caseFolder.getId(), uploads);
    }

    @Transactional
    public DocumentDTO completeUpload(UUID caseId, UUID documentId) {
        Document document = documentRepository.findByIdAndCaseFolderId(documentId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable pour ce dossier"));

        try {
            documentValidationService.validate(document);
        } catch (DocumentValidationException e) {
            document.setStatus(DocumentStatus.VALIDATION_FAILED);
            documentRepository.save(document);
            throw e;
        }

        document.setStatus(DocumentStatus.UPLOADED);
        document.setUploadedAt(Instant.now());
        document = documentRepository.save(document);

        return new DocumentDTO(document.getId(), document.getFileType(), document.getStatus(), document.getMimeType(), document.getSizeBytes(), document.getStorageUrl());
    }

    private void validateMetadata(DocumentUploadRequest request) {
        if (!StringUtils.hasText(request.filename())) {
            throw new IllegalArgumentException("Le nom du fichier est requis");
        }
        if (!StringUtils.hasText(request.mimeType()) || SUPPORTED_MIME_TYPES.stream().noneMatch(m -> m.equalsIgnoreCase(request.mimeType()))) {
            throw new IllegalArgumentException("Format de fichier non supporté : " + request.mimeType());
        }
        if (request.sizeBytes() == null || request.sizeBytes() <= 0 || request.sizeBytes() > TEN_MB) {
            throw new IllegalArgumentException("La taille du fichier doit être comprise entre 1 octet et 10 Mo");
        }
        if (!StringUtils.hasText(request.documentType())) {
            throw new IllegalArgumentException("Le type de document est requis");
        }
    }

    private String buildStorageKey(UUID caseId, UUID documentId, String filename) {
        String cleanFilename = filename == null ? "document" : filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return "cases/" + caseId + "/" + documentId + "/" + cleanFilename;
    }
}
