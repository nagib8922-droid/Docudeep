package com.example.docudeep.api;


import com.example.docudeep.api.dto.*;
import com.example.docudeep.service.ApplicationService;
import com.example.docudeep.service.CaseService;
import com.example.docudeep.service.DecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;


@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ApiController {
    private final ApplicationService applicationService;
    private final DecisionService decisionService;
    private final CaseService caseService;


    @PostMapping("/applications")
    public ResponseEntity<ApplicationDTO> create(@RequestBody @Validated ApplicationCreate req){
        return ResponseEntity.status(201).body(applicationService.create(req));
    }


    @GetMapping("/applications/{id}")
    public ApplicationDTO get(@PathVariable UUID id){
        return applicationService.get(id);
    }


    @PostMapping("/decisions")
    public DecisionDTO decide(@RequestBody @Validated DecisionRequest req){
        return decisionService.decide(req);
    }

    @PostMapping("/cases")
    public ResponseEntity<CaseCreateResponse> createCase(@RequestBody @Validated CaseCreateRequest request) {
        return ResponseEntity.status(201).body(caseService.createCase(request));
    }

    @PostMapping("/cases/{caseId}/documents/{documentId}/complete")
    public DocumentDTO completeUpload(@PathVariable UUID caseId, @PathVariable UUID documentId) {
        return caseService.completeUpload(caseId, documentId);
    }
}
