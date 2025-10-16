package com.example.docudeep.repo;

import com.example.docudeep.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByIdAndCaseFolderId(UUID documentId, UUID caseId);
}
