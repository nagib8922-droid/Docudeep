package com.example.docudeep.service.storage;

import com.example.docudeep.config.StorageProperties;
import com.example.docudeep.service.storage.dto.PresignedUpload;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development-friendly {@link StorageService} implementation that stores binaries on the local filesystem.
 * It mimics the behaviour of a pre-signed upload URL so that the same web flow can be used without AWS.
 */
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;

    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    @PostConstruct
    void ensureBaseDir() {
        try {
            Files.createDirectories(properties.getLocalBasePath());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de stockage local", e);
        }
    }

    @Override
    public PresignedUpload prepareUpload(String storageKey, String mimeType, long contentLength) {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(properties.getPresignTtl());
        sessions.put(token, new UploadSession(storageKey, mimeType, contentLength, expiry));

        Map<String, String> headers = mimeType == null || mimeType.isBlank()
                ? Collections.emptyMap()
                : Map.of("Content-Type", mimeType);

        return new PresignedUpload("/api/dev/storage/upload/" + token, "PUT", headers, properties.getPresignTtl());
    }

    @Override
    public byte[] load(String storageKey) {
        Path path = resolvePath(storageKey);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier local stocké", e);
        }
    }

    @Override
    public String resolveUrl(String storageKey) {
        return resolvePath(storageKey).toUri().toString();
    }

    public void consumeUpload(String token, byte[] payload) {
        UploadSession session = sessions.remove(token);
        if (session == null) {
            throw new IllegalArgumentException("URL de téléversement invalide ou expirée");
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("URL de téléversement expirée");
        }
        if (payload == null) {
            payload = new byte[0];
        }
        if (session.expectedSize() > 0 && payload.length > session.expectedSize()) {
            throw new IllegalArgumentException("Le fichier dépasse la taille autorisée");
        }

        Path target = resolvePath(session.storageKey());
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, payload);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le fichier localement", e);
        }
    }

    public void reset() {
        Path base = properties.getLocalBasePath();
        sessions.clear();
        try {
            FileSystemUtils.deleteRecursively(base);
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de réinitialiser le stockage local", e);
        }
    }

    private Path resolvePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey must not be blank");
        }
        return properties.getLocalBasePath().resolve(storageKey).normalize();
    }

    private record UploadSession(String storageKey, String mimeType, long expectedSize, Instant expiresAt) {}
}
