package com.arogya.cafe.support;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for every integration test: boots one real PostgreSQL container (matching production)
 * instead of H2, so dialect, constraints, and Flyway migrations are exercised exactly as they
 * run in prod. The container is a static singleton reused across all test classes (started once,
 * ~5s, then shared) — far cheaper than one container per class.
 *
 * <p>{@code @ServiceConnection} wires Spring Boot's datasource straight to the container; no JDBC
 * URL/credentials in {@code application-test.yml}. Flyway then migrates the schema and Hibernate
 * {@code ddl-auto: validate} asserts the entities match it — turning every integration test into a
 * schema-drift sensor.
 */
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * The container is shared across every test class for speed, so each test must leave it clean.
     * Truncate all application tables (not Flyway's history) and reset identity sequences after each
     * test — keeps tests independent without the cost of a fresh container or schema per class.
     */
    @AfterEach
    void resetDatabase() {
        jdbc.execute(
                """
                DO $$
                DECLARE r RECORD;
                BEGIN
                  FOR r IN (
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                  ) LOOP
                    EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
                  END LOOP;
                END $$;
                """);
    }
}
