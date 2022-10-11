package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.runtime.Converters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class ConvertTest {

    private final static LocalDateTime LOCAL_DATE_TIME =
            LocalDateTime.of(2022, 10, 10, 13, 43);

    @Test
    public void testConvertJavaSqlDate() {
        LocalDateTime datetime = (LocalDateTime)
                Converters.tryConvert(
                        java.sql.Date.valueOf(LOCAL_DATE_TIME.toLocalDate()),
                        LocalDateTime.class
                );
        Assertions.assertEquals(
                "2022-10-10T00:00",
                datetime.toString()
        );
    }

    @Test
    public void testConvertJavaSqlTime() {
        LocalDateTime datetime = (LocalDateTime)
                Converters.tryConvert(
                        java.sql.Time.valueOf(LOCAL_DATE_TIME.toLocalTime()),
                        LocalDateTime.class
                );
        Assertions.assertEquals(
                "1970-01-01T13:43",
                datetime.toString()
        );
    }

    @Test
    public void testConvertJavaSqlTimestamp() {
        LocalDateTime datetime = (LocalDateTime)
                Converters.tryConvert(
                        java.sql.Timestamp.valueOf(LOCAL_DATE_TIME),
                        LocalDateTime.class
                );
        Assertions.assertEquals(
                "2022-10-10T13:43",
                datetime.toString()
        );
    }

    @Test
    public void testConvertJavaUtilDate() {
        LocalDateTime datetime = (LocalDateTime)
                Converters.tryConvert(
                        java.util.Date.from(LOCAL_DATE_TIME.toInstant(OffsetDateTime.now().getOffset())),
                        LocalDateTime.class
                );
        Assertions.assertEquals(
                "2022-10-10T13:43",
                datetime.toString()
        );
    }
}
