package com.example.docudeep.repo;

import com.example.docudeep.CaseFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseFolderRepository extends JpaRepository<CaseFolder, UUID> {
}
