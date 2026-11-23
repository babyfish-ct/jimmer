package org.babyfish.jimmer;

import org.babyfish.jimmer.json.ModelJson;
import org.babyfish.jimmer.model.Complex;
import org.babyfish.jimmer.model.TimeData;
import org.babyfish.jimmer.model.TimeDataDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimeDataTest {

    @JacksonTest
    public void test(JacksonBackend backend) {
        LocalDateTime time = LocalDateTime.of(2022, 9, 13, 23, 49, 34);
        TimeData timeData = TimeDataDraft.$.produce(data -> {
            data.setTime1(Date.from(time.atOffset(ZoneOffset.ofHours(8)).toInstant()));
            data.setTime2(Date.from(time.atOffset(ZoneOffset.ofHours(8)).toInstant()));
            data.setTime3(time);
            data.setTime4(time);
            data.setTime5(time.toLocalDate());
            data.setTime6(time.toLocalDate());
            data.setTime7(time);
        });
        String json = backend.write(timeData);
        Assertions.assertEquals(
                "{" +
                        "\"time1\":\"2022-09-13 15:49:34\"," +
                        "\"time2\":\"2022/09/13 15:49\"," +
                        "\"time3\":\"2022-09-13 23:49:34\"," +
                        "\"time4\":\"2022/09/13 23:49\"," +
                        "\"time5\":\"2022-09-13\"," +
                        "\"time6\":\"2022/09/13\"," +
                        "\"time7\":\"2022年09月13日23时49分34秒\"" +
                        "}",
                json
        );
        TimeData timeData2 = backend.read(TimeData.class, json);
        Assertions.assertEquals(
                json,
                backend.write(timeData2)
        );
    }

    @Test
    public void testFormatInSharedBinding() {
        ModelJson.Conversion fallback = (type, value) -> {
            throw new AssertionError("Expected a formatted property of type " + type);
        };
        Map<String, Object> dates = new LinkedHashMap<>();
        dates.put("time1", "2022-09-13 15:49:34");
        dates.put("time5", "2022-09-13");
        dates.put("time7", "2022年09月13日23时49分34秒");
        TimeData time = (TimeData) ModelJson.decode(TimeData.class, dates, fallback);
        Assertions.assertEquals("2022-09-13T15:49:34Z", time.time1().toInstant().toString());
        Assertions.assertEquals(dates, ModelJson.encode(TimeData.class, time, fallback));

        Map<String, Object> numbers = Collections.singletonMap("real", "5.0");
        Complex number = (Complex) ModelJson.decode(Complex.class, numbers, fallback);
        Assertions.assertEquals(5.0, number.getReal());
        Assertions.assertEquals(numbers, ModelJson.encode(Complex.class, number, fallback));
    }
}
