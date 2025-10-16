package com.example.docudeep.api;


import com.example.docudeep.api.dto.*;
import com.example.docudeep.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;


@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ApiController {
    private final ApplicationService applicationService;
    public final DecisionService decisionService;


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
}
