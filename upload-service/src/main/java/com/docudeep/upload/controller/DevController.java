package com.docudeep.upload.controller;

import com.docudeep.upload.service.CaseStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/dev")
public class DevController {

    private final CaseStorageService storageService;

    public DevController(CaseStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/storage/reset")
    public ResponseEntity<Void> resetStorage() throws IOException {
        storageService.reset();
        return ResponseEntity.noContent().build();
    }
}
