package com.example.docudeep;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Applicant {
    @Id @UuidGenerator
    private UUID id;
    private String fullName;
    @Column(unique = true) private String email;
    private String kycStatus;
}
