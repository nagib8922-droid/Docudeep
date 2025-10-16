// src/main/java/com/example/mvp/Bootstrap.java
package com.example.docudeep;

import com.example.docudeep.repo.ApplicantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Bootstrap {
    @Bean
    CommandLineRunner seed(ApplicantRepository repo){
        return args -> {
            if (repo.count() == 0) {
                repo.save(Applicant.builder()
                        .fullName("Ada Lovelace")
                        .email("ada@example.com")
                        .kycStatus("VERIFIED")
                        .build());
                System.out.println("Seeded: Ada Lovelace (applicant)");
            }
        };
    }
}
