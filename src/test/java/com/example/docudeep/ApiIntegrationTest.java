package com.example.docudeep;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
class ApiIntegrationTest {

    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @BeforeAll
    static void start() { pg.start(); }

    @AfterAll
    static void stop() { pg.stop(); }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void contextLoads() { }
}
