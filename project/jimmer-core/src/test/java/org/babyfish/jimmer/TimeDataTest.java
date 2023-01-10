package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.model.TimeData;
import org.babyfish.jimmer.model.TimeDataDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

public class TimeDataTest {

    @Test
    public void test() throws JsonProcessingException {
        LocalDateTime time = LocalDateTime.of(2022, 9, 13, 23, 49, 34);
        TimeData timeData = TimeDataDraft.$.produce(data -> {
            data.setTime1(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
            data.setTime2(Date.from(time.atZone(ZoneId.systemDefault()).toInstant()));
            data.setTime3(time);
            data.setTime4(time);
            data.setTime5(time.toLocalDate());
            data.setTime6(time.toLocalDate());
        });
        String json = timeData.toString();
        Assertions.assertEquals(
                "{" +
                        "\"time1\":\"2022-09-13 15:49:34\"," +
                        "\"time2\":\"2022/09/13 15:49\"," +
                        "\"time3\":\"2022-09-13 23:49:34\"," +
                        "\"time4\":\"2022/09/13 23:49\"," +
                        "\"time5\":\"2022-09-13\"," +
                        "\"time6\":\"2022/09/13\"" +
                        "}",
                json
        );
        TimeData timeData2 = ImmutableObjects.fromString(TimeData.class, json);
        Assertions.assertEquals(
                json,
                timeData2.toString()
        );
    }

    @Test
    public void testNoTime() {
        TimeData timeData = TimeDataDraft.$.produce(data -> {});
        Assertions.assertEquals("{}", timeData.toString());
    }
}
