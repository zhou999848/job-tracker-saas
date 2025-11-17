// src/test/java/com/example/jobtracker/container/PostgresTCBase.java
package com.example.jobtracker.container;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class PostgresTCBase {

    @Container
    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("jobtracker_test")
            .withUsername("test")
            .withPassword("test");
}