package org.babyfish.jimmer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.babyfish.jimmer.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

@Immutable
public interface TimeData {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    Date time1();

    @JsonFormat(pattern = "yyyy/MM/dd HH:mm")
    Date time2();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime time3();

    @JsonFormat(pattern = "yyyy/MM/dd HH:mm")
    LocalDateTime time4();

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate time5();

    @JsonFormat(pattern = "yyyy/MM/dd")
    LocalDate time6();
}
