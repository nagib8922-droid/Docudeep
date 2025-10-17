package com.docudeep.upload.service;

import com.docudeep.upload.config.StorageProperties;
import com.docudeep.upload.model.CaseMetadata;
import com.docudeep.upload.model.DocumentMetadata;
import com.docudeep.upload.model.DocumentStatus;
import com.docudeep.upload.model.DocumentType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CaseStorageService {

    private static final TypeReference<CaseMetadata> CASE_METADATA_TYPE = new TypeReference<>() {};

    private final Path root;
    private final ObjectMapper objectMapper;

    public CaseStorageService(StorageProperties properties, ObjectMapper objectMapper) throws IOException {
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        Files.createDirectories(root);
    }

    public CaseMetadata createCase(List<DocumentRequest> documents) throws IOException {
        String caseId = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Path caseDir = ensureCaseDirectory(caseId);

        CaseMetadata metadata = new CaseMetadata();
        metadata.setCaseId(caseId);
        metadata.setCreatedAt(Instant.now());
        List<DocumentMetadata> documentMetadata = new ArrayList<>();

        for (DocumentRequest request : documents) {
            DocumentMetadata doc = new DocumentMetadata();
            doc.setId(UUID.randomUUID().toString());
            doc.setFilename(request.filename());
            doc.setMimeType(request.mimeType());
            doc.setDeclaredSize(request.sizeBytes());
            doc.setDocumentType(request.documentType());
            doc.setStatus(DocumentStatus.PENDING);
            documentMetadata.add(doc);
        }

        metadata.setDocuments(documentMetadata);
        writeMetadata(caseDir, metadata);
        return metadata;
    }

    public Optional<CaseMetadata> findCase(String caseId) {
        Path metadataFile = caseMetadataFile(caseId);
        if (!Files.exists(metadataFile)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(metadataFile.toFile(), CASE_METADATA_TYPE));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read case metadata", e);
        }
    }

    public void storeDocument(String caseId, String documentId, InputStream data) throws IOException {
        CaseMetadata metadata = findCase(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        DocumentMetadata document = metadata.getDocuments().stream()
            .filter(doc -> doc.getId().equals(documentId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        Path caseDir = ensureCaseDirectory(caseId);
        String storageFilename = document.getId() + "_" + document.getFilename();
        Path destination = caseDir.resolve(storageFilename);

        long storedBytes = Files.copy(data, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        document.setStorageFilename(storageFilename);
        document.setStoredSize(storedBytes);
        document.setUploadedAt(Instant.now());
        document.setStatus(DocumentStatus.UPLOADED);
        document.setFailureReason(null);

        writeMetadata(caseDir, metadata);
    }

    public DocumentMetadata validateDocument(String caseId, String documentId) throws IOException {
        CaseMetadata metadata = findCase(caseId)
            .orElseThrow(() -> new IllegalArgumentException("Case not found"));

        DocumentMetadata document = metadata.getDocuments().stream()
            .filter(doc -> doc.getId().equals(documentId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (document.getStorageFilename() == null) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason("Document not uploaded");
        } else if (document.getStoredSize() <= 0) {
            document.setStatus(DocumentStatus.FAILED);
            document.setFailureReason("Fichier vide");
        } else {
            document.setStatus(DocumentStatus.VALIDATED);
            document.setFailureReason(null);
        }

        writeMetadata(ensureCaseDirectory(caseId), metadata);
        return document;
    }

    public void reset() throws IOException {
        FileSystemUtils.deleteRecursively(root);
        Files.createDirectories(root);
    }

    public Path resolveDocumentPath(String caseId, String documentId) {
        return findCase(caseId)
            .flatMap(caseMetadata -> caseMetadata.getDocuments().stream()
                .filter(doc -> doc.getId().equals(documentId))
                .findFirst())
            .map(DocumentMetadata::getStorageFilename)
            .map(filename -> root.resolve(caseId).resolve(filename))
            .orElseThrow(() -> new IllegalArgumentException("Document not found"));
    }

    public List<CaseMetadata> listCases() throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }

        List<CaseMetadata> cases = new ArrayList<>();
        try (var directories = Files.list(root)) {
            directories.filter(Files::isDirectory).forEach(caseDir -> {
                Path metadataFile = caseDir.resolve("metadata.json");
                if (Files.exists(metadataFile)) {
                    try {
                        cases.add(objectMapper.readValue(metadataFile.toFile(), CASE_METADATA_TYPE));
                    } catch (IOException ignored) {
                        // Skip unreadable metadata files
                    }
                }
            });
        }
        return cases;
    }

    private Path ensureCaseDirectory(String caseId) throws IOException {
        Path caseDir = root.resolve(caseId);
        Files.createDirectories(caseDir);
        return caseDir;
    }

    private Path caseMetadataFile(String caseId) {
        return root.resolve(caseId).resolve("metadata.json");
    }

    private void writeMetadata(Path caseDir, CaseMetadata metadata) throws IOException {
        Path file = caseDir.resolve("metadata.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), metadata);
    }

    public record DocumentRequest(String filename, String mimeType, long sizeBytes, DocumentType documentType) {}
}
