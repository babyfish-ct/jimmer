package org.babyfish.jimmer;

import org.babyfish.jimmer.model.LongData;
import org.babyfish.jimmer.model.LongDataDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.Arrays;

public class LongDataTest {

    @JacksonTest
    public void test(JacksonBackend backend) {
        LongData data = LongDataDraft.$.produce(draft -> {
            draft.setNotNullValue(1L);
            draft.setNullableValue(2L);
            draft.setValues(Arrays.asList(3L, 4L));
        });
        String json = backend.write(data);
        Assertions.assertEquals(
                "{\"notNullValue\":\"1\",\"nullableValue\":\"2\",\"values\":[\"3\",\"4\"]}",
                json
        );
        LongData data2 = backend.read(LongData.class, json);
        Assertions.assertEquals(
                data,
                data2
        );
    }
}
