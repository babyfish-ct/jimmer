package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Data;
import org.babyfish.jimmer.model.DataDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.Arrays;

public class DataTest {

    @JacksonTest
    public void testSerialize(JacksonBackend backend) {
        Data data = DataDraft.$.produce(draft -> {
            draft.setList(Arrays.asList(1L, 2L));
            draft.setNestedList(
                    Arrays.asList(
                            Arrays.asList(3L, 4L),
                            Arrays.asList(5L, 6L)
                    )
            );
            draft.setArr(new long[]{7L, 8L});
            draft.setNestedArr(
                    new long[][]{
                            {9L, 10L},
                            {11L, 12L}
                    }
            );
        });
        Assertions.assertEquals(
                "{\"list\":[1,2]," +
                        "\"nestedList\":[[3,4],[5,6]]," +
                        "\"arr\":[7,8]," +
                        "\"nestedArr\":[[9,10],[11,12]]}",
                backend.write(data)
        );
    }

    @JacksonTest
    public void testDeserialize(JacksonBackend backend) {
        String json = "{\"list\":[1,2]," +
                "\"nestedList\":[[3,4],[5,6]]," +
                "\"arr\":[7,8]," +
                "\"nestedArr\":[[9,10],[11,12]]}";
        Data data = backend.read(Data.class, json);
        Assertions.assertEquals(json, backend.write(data));
    }
}
