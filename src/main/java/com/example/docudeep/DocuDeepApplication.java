package com.example.docudeep;

import com.example.docudeep.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class DocuDeepApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocuDeepApplication.class, args);
    }

}
