package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.model.Data;
import org.babyfish.jimmer.model.DataDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class DataTest {

    @Test
    public void testSerialize() {
        Data data = DataDraft.$.produce(draft -> {
            draft.setList(Arrays.asList(1L, 2L));
            draft.setNestedList(
                    Arrays.asList(
                            Arrays.asList(3L, 4L),
                            Arrays.asList(5L, 6L)
                    )
            );
            draft.setArr(new long[] { 7L, 8L });
            draft.setNestedArr(
                    new long[][] {
                            { 9L, 10L },
                            { 11L, 12L }
                    }
            );
        });
        Assertions.assertEquals(
                "{\"list\":[1,2]," +
                        "\"nestedList\":[[3,4],[5,6]]," +
                        "\"arr\":[7,8]," +
                        "\"nestedArr\":[[9,10],[11,12]]}",
                data.toString()
        );
    }

    @Test
    public void testDeserialize() throws JsonProcessingException {
        String json = "{\"list\":[1,2]," +
                "\"nestedList\":[[3,4],[5,6]]," +
                "\"arr\":[7,8]," +
                "\"nestedArr\":[[9,10],[11,12]]}";
        Data data = ImmutableObjects.fromString(Data.class, json);
        Assertions.assertEquals(json, data.toString());
    }
}
