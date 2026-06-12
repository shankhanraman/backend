package com.arogya.cafe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.arogya.cafe.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Migration-safety guard. Booting this {@code @SpringBootTest} against the real PostgreSQL
 * container already does the heavy lifting: Flyway applies every {@code V*.sql} from scratch and
 * Hibernate {@code ddl-auto: validate} fails context load if any entity disagrees with the
 * migrated schema (catching entity/schema drift that H2 could hide). This test additionally
 * asserts Flyway's own state is clean.
 */
@SpringBootTest
class MigrationConsistencyTest extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void allMigrationsApplyCleanlyAndChecksumsMatch() {
        // Throws if any applied migration's checksum drifted from the script on disk
        // (i.e. someone edited an already-applied V*.sql instead of adding a new one).
        flyway.validate();

        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 2, "expected at least V1 and V2 to be applied");
    }
}
