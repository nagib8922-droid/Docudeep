package com.example.docudeep.service;

import com.example.docudeep.Document;
import com.example.docudeep.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DocumentValidationService {

    private final StorageService storageService;

    public void validate(Document document) {
        byte[] payload;
        try {
            payload = storageService.load(document.getStorageKey());
        } catch (RuntimeException ex) {
            throw new DocumentValidationException("Impossible de récupérer le fichier stocké", ex);
        }
        if (payload == null || payload.length == 0) {
            throw new DocumentValidationException("Le fichier est vide ou inaccessible");
        }

        String mimeType = document.getMimeType();
        if (mimeType == null) {
            throw new DocumentValidationException("Type MIME manquant pour le document");
        }

        if (mimeType.equalsIgnoreCase("application/pdf")) {
            validatePdf(payload);
        } else if (mimeType.equalsIgnoreCase("image/png") || mimeType.equalsIgnoreCase("image/jpeg")) {
            validateImage(payload);
        } else {
            throw new DocumentValidationException("Type de fichier non supporté: " + mimeType);
        }
    }

    private void validatePdf(byte[] payload) {
        try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(payload))) {
            if (pdf.isEncrypted()) {
                throw new DocumentValidationException("Le document PDF est protégé par mot de passe");
            }
        } catch (InvalidPasswordException e) {
            throw new DocumentValidationException("Le document PDF est protégé par mot de passe", e);
        } catch (IOException e) {
            throw new DocumentValidationException("Impossible de lire le document PDF", e);
        }
    }

    private void validateImage(byte[] payload) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(payload));
            if (image == null) {
                throw new DocumentValidationException("Le fichier image est illisible");
            }
        } catch (IOException e) {
            throw new DocumentValidationException("Impossible de lire le fichier image", e);
        }
    }
}
