package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class VariablesTest extends AbstractQueryTest {

    // issue #1366: Instant must be normalized to OffsetDateTime at UTC, not to java.sql.Timestamp,
    // whose toString() formats in the JVM default zone and made logged SQL show local wall-clock.
    @Test
    public void testInstantNormalizedToUtcOffsetDateTime() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        Instant instant = Instant.parse("2026-04-19T00:00:00Z");

        Object processed = Variables.process(instant, Instant.class, sqlClient);

        Assertions.assertInstanceOf(OffsetDateTime.class, processed, "Instant should be normalized to OffsetDateTime, got " + processed.getClass());
        OffsetDateTime odt = (OffsetDateTime) processed;
        Assertions.assertEquals(ZoneOffset.UTC, odt.getOffset());
        Assertions.assertEquals(instant, odt.toInstant());
        Assertions.assertTrue(
                odt.toString().endsWith("Z"),
                "OffsetDateTime should render in UTC, got " + odt
        );
    }

    // Regression guard: other temporal types keep their original conversion.
    @Test
    public void testLocalDateTimeStillTimestamp() {
        JSqlClientImplementor sqlClient = (JSqlClientImplementor) getSqlClient();
        LocalDateTime ldt = LocalDateTime.of(2026, 4, 19, 12, 0, 0);

        Object processed = Variables.process(ldt, LocalDateTime.class, sqlClient);

        Assertions.assertInstanceOf(Timestamp.class, processed, "LocalDateTime should still convert to java.sql.Timestamp, got " + processed.getClass());
    }
}
