package com.example.docudeep.repo;

import com.example.docudeep.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicantRepository extends JpaRepository<Applicant, UUID> {
}
