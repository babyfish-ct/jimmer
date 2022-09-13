package org.babyfish.jimmer;

import org.babyfish.jimmer.model.TimeData;
import org.babyfish.jimmer.model.TimeDataDraft;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

public class TimeDataTest {

    @Test
    public void test() {
        LocalDateTime time = LocalDateTime.of(2022, 9, 13, 23, 49, 34);
        TimeData timeData = TimeDataDraft.$.produce(data -> {
            data.setTime1(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
            data.setTime2(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
            data.setTime3(time);
            data.setTime4(time);
            data.setTime5(time.atZone(ZoneId.systemDefault()).toOffsetDateTime());
            data.setTime6(time.atZone(ZoneId.systemDefault()).toOffsetDateTime());
            data.setTime7(time.toLocalDate());
            data.setTime8(time.toLocalDate());
        });
        System.out.println(timeData);
    }
}
