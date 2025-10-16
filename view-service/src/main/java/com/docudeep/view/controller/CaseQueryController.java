package com.docudeep.view.controller;

import com.docudeep.view.config.StorageProperties;
import com.docudeep.view.model.CaseMetadata;
import com.docudeep.view.model.DocumentMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
public class CaseQueryController {

    private static final TypeReference<CaseMetadata> CASE_METADATA_TYPE = new TypeReference<>() {};

    private final Path root;
    private final ObjectMapper objectMapper;

    public CaseQueryController(StorageProperties properties, ObjectMapper objectMapper) {
        this.root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    @GetMapping(path = "/cases", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CaseMetadata> listCases() throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var directories = Files.list(root)) {
            return directories
                .filter(Files::isDirectory)
                .map(this::readCaseMetadata)
                .flatMap(Optional::stream)
                .toList();
        }
    }

    @GetMapping(path = "/cases/{caseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCase(@PathVariable String caseId) throws IOException {
        return readCaseMetadata(root.resolve(caseId)).<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                "message", "Dossier introuvable"
            )));
    }

    @GetMapping(path = "/cases/{caseId}/documents/{documentId}")
    public ResponseEntity<Resource> download(@PathVariable String caseId, @PathVariable String documentId) throws IOException {
        Optional<CaseMetadata> metadataOpt = readCaseMetadata(root.resolve(caseId));
        if (metadataOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CaseMetadata metadata = metadataOpt.get();
        DocumentMetadata document = metadata.getDocuments().stream()
            .filter(doc -> documentId.equals(doc.getId()))
            .findFirst()
            .orElse(null);
        if (document == null || document.getStorageFilename() == null) {
            return ResponseEntity.notFound().build();
        }
        Path file = root.resolve(caseId).resolve(document.getStorageFilename());
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String filename = document.getFilename();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (document.getMimeType() != null) {
            mediaType = MediaType.parseMediaType(document.getMimeType());
        }

        return ResponseEntity.ok()
            .contentLength(resource.contentLength())
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(mediaType)
            .body(resource);
    }

    private Optional<CaseMetadata> readCaseMetadata(Path directory) {
        Path metadataFile = directory.resolve("metadata.json");
        if (!Files.exists(metadataFile)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(metadataFile.toFile());
            CaseMetadata metadata = objectMapper.readValue(bytes, CASE_METADATA_TYPE);
            return Optional.of(metadata);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
