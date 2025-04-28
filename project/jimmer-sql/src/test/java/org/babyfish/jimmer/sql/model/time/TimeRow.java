package org.babyfish.jimmer.sql.model.time;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;

import java.time.*;

@Entity
public interface TimeRow {

    @Id
    long id();

    java.util.Date value1();

    java.sql.Date value2();

    java.sql.Time value3();

    java.sql.Timestamp value4();

    LocalDate value5();

    LocalTime value6();

    LocalDateTime value7();

    OffsetDateTime value8();

    ZonedDateTime value9();
}
