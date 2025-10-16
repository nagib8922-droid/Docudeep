package com.example.docudeep.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.regions.Region;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@ConfigurationProperties(prefix = "docudeep.storage")
@Validated
@Data
public class StorageProperties {

    @NotBlank
    private String mode = "s3";

    @NotBlank
    private String bucket = "docudeep-local";

    @NotBlank
    private String region = Region.EU_WEST_3.id();

    @NotNull
    private Duration presignTtl = Duration.ofMinutes(15);

    /**
     * Local directory used when {@link #mode} is set to {@code local}.
     */
    @NotBlank
    private String localBasePath = "storage";

    public Region getRegion() {
        return Region.of(region);
    }

    public boolean isLocalMode() {
        return "local".equalsIgnoreCase(mode);
    }

    public Path getLocalBasePath() {
        return Paths.get(localBasePath).toAbsolutePath().normalize();
    }
}
