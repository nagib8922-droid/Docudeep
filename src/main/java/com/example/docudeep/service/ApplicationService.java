package com.example.docudeep.service;

import com.example.docudeep.Applicant;
import com.example.docudeep.Application;
import com.example.docudeep.api.dto.ApplicationCreate;
import com.example.docudeep.api.dto.ApplicationDTO;
import com.example.docudeep.api.mapper.ApplicationMapper;
import com.example.docudeep.repo.ApplicantRepository;
import com.example.docudeep.repo.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicantRepository applicantRepo;
    private final ApplicationRepository applicationRepo;
    private final ApplicationMapper mapper;


    @Transactional
    public ApplicationDTO create(ApplicationCreate req) {
        Applicant applicant = applicantRepo.findById(req.applicantId())
                .orElseThrow(() -> new IllegalArgumentException("Applicant not found"));
        Application app = mapper.toEntity(req, applicant);
        return mapper.toDTO(applicationRepo.save(app));
    }


    @Transactional(readOnly = true)
    public ApplicationDTO get(UUID id) {
        return applicationRepo.findById(id).map(mapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
    }
}
