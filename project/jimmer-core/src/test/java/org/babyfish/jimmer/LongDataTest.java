package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.model.LongData;
import org.babyfish.jimmer.model.LongDataDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class LongDataTest {

    @Test
    public void test() throws JsonProcessingException {
        LongData data = LongDataDraft.$.produce(draft -> {
            draft.setNotNullValue(1L);
            draft.setNullableValue(2L);
            draft.setValues(Arrays.asList(3L, 4L));
        });
        String json = data.toString();
        Assertions.assertEquals(
                "{\"notNullValue\":\"1\",\"nullableValue\":\"2\",\"values\":[\"3\",\"4\"]}",
                json
        );
        LongData data2 = ImmutableObjects.fromString(LongData.class, json);
        Assertions.assertEquals(
                data,
                data2
        );
    }
}
