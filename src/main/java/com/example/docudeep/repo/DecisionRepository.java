package com.example.docudeep.repo;
import com.example.docudeep.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;


public interface DecisionRepository extends JpaRepository<Decision, UUID> {}
