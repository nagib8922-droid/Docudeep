package com.example.docudeep.service;

import com.example.docudeep.api.dto.DecisionDTO;
import com.example.docudeep.api.dto.DecisionRequest;
import com.example.docudeep.Application;
import com.example.docudeep.Decision;
import com.example.docudeep.repo.ApplicationRepository;
import com.example.docudeep.repo.DecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final ApplicationRepository applicationRepo;
    private final DecisionRepository decisionRepo;

    @Transactional
    public DecisionDTO decide(DecisionRequest req) {
        Application app = applicationRepo.findById(req.applicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        double score = Math.max(0, Math.min(100, 100 - (app.getAmount() / 1000.0))); // mock rule
        String outcome = score >= 60 ? "APPROVED" : (score >= 40 ? "MANUAL_REVIEW" : "REJECTED");

        Decision d = Decision.builder()
                .application(app)
                .score(score)
                .outcome(outcome)
                .reason("mock-rule")
                .decidedAt(java.time.OffsetDateTime.now())
                .build();

        decisionRepo.save(d);
        return new DecisionDTO(app.getId(), score, outcome, d.getReason());
    }
}
