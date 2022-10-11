package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.runtime.Converters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class ConvertTest {

    private final static LocalDateTime LOCAL_DATE_TIME =
            LocalDateTime.of(2022, 10, 10, 13, 43);

    @Test
    public void testConvertFromJavaSqlDate() {
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
    public void testConvertFromJavaSqlTime() {
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
    public void testConvertFromJavaSqlTimestamp() {
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
    public void testConvertFromJavaUtilDate() {
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

    @Test
    public void testConvertToJavaSqlDate() {
        java.sql.Date date = (java.sql.Date)Converters.tryConvert(
                LOCAL_DATE_TIME,
                java.sql.Date.class
        );
        Assertions.assertEquals(
                "2022-10-10",
                date.toString()
        );
    }

    @Test
    public void testConvertToJavaSqlTime() {
        java.sql.Time time = (java.sql.Time)Converters.tryConvert(
                LOCAL_DATE_TIME,
                java.sql.Time.class
        );
        Assertions.assertEquals(
                "13:43:00",
                time.toString()
        );
    }
    @Test
    public void testConvertToJavaSqlTimestamp() {
        java.sql.Timestamp timestamp = (java.sql.Timestamp)Converters.tryConvert(
                LOCAL_DATE_TIME,
                java.sql.Timestamp.class
        );
        Assertions.assertEquals(
                "2022-10-10 13:43:00.0",
                timestamp.toString()
        );
    }

    @Test
    public void testConvertToJavaUtilDate() {
        java.util.Date date = (java.util.Date)Converters.tryConvert(
                LOCAL_DATE_TIME,
                java.util.Date.class
        );
        Assertions.assertEquals(
                "2022-10-10 13:43:00",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
        );
    }
}
