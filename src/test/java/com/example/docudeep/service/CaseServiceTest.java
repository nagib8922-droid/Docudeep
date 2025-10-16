package com.example.docudeep.service;

import com.example.docudeep.CaseFolder;
import com.example.docudeep.Document;
import com.example.docudeep.DocumentStatus;
import com.example.docudeep.DocumentType;
import com.example.docudeep.api.dto.CaseCreateRequest;
import com.example.docudeep.api.dto.DocumentUploadRequest;
import com.example.docudeep.repo.CaseFolderRepository;
import com.example.docudeep.repo.DocumentRepository;
import com.example.docudeep.service.storage.StorageService;
import com.example.docudeep.service.storage.dto.PresignedUpload;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private CaseFolderRepository caseFolderRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private StorageService storageService;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        DocumentValidationService documentValidationService = new DocumentValidationService(storageService);
        caseService = new CaseService(caseFolderRepository, documentRepository, storageService, documentValidationService);

        when(caseFolderRepository.save(any(CaseFolder.class))).thenAnswer(invocation -> {
            CaseFolder folder = invocation.getArgument(0);
            if (folder.getId() == null) {
                folder.setId(UUID.randomUUID());
            }
            return folder;
        });

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId(UUID.randomUUID());
            }
            return document;
        });
    }

    @Test
    void shouldCreateCaseAndReturnUploadPlan() {
        when(storageService.prepareUpload(anyString(), anyString(), anyLong()))
                .thenReturn(new PresignedUpload("https://upload", "PUT", Map.of(), Duration.ofMinutes(15)));
        when(storageService.resolveUrl(anyString())).thenAnswer(invocation -> "s3://bucket/" + invocation.getArgument(0));

        CaseCreateRequest request = new CaseCreateRequest(List.of(
                new DocumentUploadRequest("payslip.pdf", "application/pdf", 1024L, "PAYSLIP"),
                new DocumentUploadRequest("avis.jpg", "image/jpeg", 2048L, "avis d'imposition")
        ));

        var response = caseService.createCase(request);

        assertThat(response.caseId()).isNotNull();
        assertThat(response.uploads()).hasSize(2);
        assertThat(response.uploads()).allSatisfy(upload -> {
            assertThat(upload.uploadUrl()).isEqualTo("https://upload");
            assertThat(upload.method()).isEqualTo("PUT");
        });

        verify(documentRepository, atLeast(2)).save(any(Document.class));
    }

    @Test
    void shouldRejectWhenTooManyDocuments() {
        List<DocumentUploadRequest> documents = List.of(
                new DocumentUploadRequest("1.pdf", "application/pdf", 100L, "PAYSLIP"),
                new DocumentUploadRequest("2.pdf", "application/pdf", 100L, "PAYSLIP"),
                new DocumentUploadRequest("3.pdf", "application/pdf", 100L, "PAYSLIP"),
                new DocumentUploadRequest("4.pdf", "application/pdf", 100L, "PAYSLIP"),
                new DocumentUploadRequest("5.pdf", "application/pdf", 100L, "PAYSLIP"),
                new DocumentUploadRequest("6.pdf", "application/pdf", 100L, "PAYSLIP")
        );

        assertThatThrownBy(() -> caseService.createCase(new CaseCreateRequest(documents)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum 5 documents");
    }

    @Test
    void shouldValidateAndMarkDocumentUploaded() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        CaseFolder folder = new CaseFolder();
        folder.setId(caseId);
        document.setCaseFolder(folder);
        document.setMimeType("application/pdf");
        document.setStorageKey("cases/" + caseId + "/" + documentId + "/file.pdf");
        document.setFileType(DocumentType.PAYSLIP);
        document.setSizeBytes(1024L);

        when(documentRepository.findByIdAndCaseFolderId(documentId, caseId)).thenReturn(Optional.of(document));
        when(storageService.load(document.getStorageKey())).thenReturn(createSimplePdf(false));
        when(storageService.resolveUrl(anyString())).thenReturn("s3://bucket/file.pdf");
        when(storageService.prepareUpload(anyString(), anyString(), anyLong())).thenReturn(new PresignedUpload("https://upload", "PUT", Map.of(), Duration.ofMinutes(15)));

        var dto = caseService.completeUpload(caseId, documentId);

        assertThat(dto.status()).isEqualTo(DocumentStatus.UPLOADED);
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeast(1)).save(captor.capture());
        assertThat(captor.getAllValues()).anySatisfy(saved -> assertThat(saved.getStatus()).isEqualTo(DocumentStatus.UPLOADED));
    }

    @Test
    void shouldFlagPasswordProtectedPdf() throws Exception {
        UUID caseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        CaseFolder folder = new CaseFolder();
        folder.setId(caseId);
        document.setCaseFolder(folder);
        document.setMimeType("application/pdf");
        document.setStorageKey("cases/" + caseId + "/" + documentId + "/file.pdf");
        document.setFileType(DocumentType.PAYSLIP);
        document.setSizeBytes(1024L);

        when(documentRepository.findByIdAndCaseFolderId(documentId, caseId)).thenReturn(Optional.of(document));
        when(storageService.load(document.getStorageKey())).thenReturn(createSimplePdf(true));

        assertThatThrownBy(() -> caseService.completeUpload(caseId, documentId))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("mot de passe");

        verify(documentRepository, atLeastOnce()).save(argThat(doc -> doc.getStatus() == DocumentStatus.VALIDATION_FAILED));
    }

    private byte[] createSimplePdf(boolean passwordProtected) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            if (passwordProtected) {
                AccessPermission ap = new AccessPermission();
                StandardProtectionPolicy policy = new StandardProtectionPolicy("owner", "secret", ap);
                policy.setEncryptionKeyLength(128);
                policy.setPermissions(ap);
                doc.protect(policy);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
