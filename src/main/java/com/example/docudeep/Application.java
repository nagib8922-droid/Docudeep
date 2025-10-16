package com.example.docudeep;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {
    @Id @UuidGenerator
    private UUID id;


    @ManyToOne(optional = false)
    private Applicant applicant;


    private Double amount;
    private String status;
    private OffsetDateTime createdAt;
}
