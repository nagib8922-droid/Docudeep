package com.example.docudeep.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.regions.Region;

import java.time.Duration;

@ConfigurationProperties(prefix = "docudeep.storage")
@Validated
@Data
public class StorageProperties {

    @NotBlank
    private String bucket;

    @NotBlank
    private String region = Region.EU_WEST_3.id();

    @NotNull
    private Duration presignTtl = Duration.ofMinutes(15);

    public Region getRegion() {
        return Region.of(region);
    }
}
