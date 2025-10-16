package com.example.docudeep.api;

import com.example.docudeep.service.storage.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/storage")
@RequiredArgsConstructor
@ConditionalOnBean(LocalStorageService.class)
public class LocalStorageController {

    private final LocalStorageService storageService;

    @PutMapping("/upload/{token}")
    public ResponseEntity<Void> upload(@PathVariable String token, @RequestBody byte[] payload) {
        storageService.consumeUpload(token, payload);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        storageService.reset();
        return ResponseEntity.noContent().build();
    }
}
