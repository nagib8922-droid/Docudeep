package com.example.docudeep;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Decision {
    @Id @UuidGenerator
    private UUID id;


    @ManyToOne(optional = false)
    private Application application;


    private Double score;
    private String outcome;
    private String reason;
    private OffsetDateTime decidedAt;
}
