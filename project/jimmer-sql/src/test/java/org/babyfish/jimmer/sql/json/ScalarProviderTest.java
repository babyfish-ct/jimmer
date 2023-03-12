package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.model.pg.JsonWrapper;
import org.babyfish.jimmer.sql.model.pg.JsonWrapperDraft;
import org.babyfish.jimmer.sql.model.pg.JsonWrapperTable;
import org.babyfish.jimmer.sql.model.pg.Point;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ScalarProviderTest extends AbstractJsonTest {

    @Test
    public void test() {
        sqlClient().getEntities().save(
                JsonWrapperDraft.$.produce(draft -> {
                    draft.setId(1L);
                    draft.setPoint(new Point(3, 4));
                    draft.setTags(Arrays.asList("java", "kotlin"));
                })
        );
        JsonWrapper wrapper = sqlClient().getEntities().findById(JsonWrapper.class, 1L);
        Assertions.assertEquals(
                "{\"id\":1,\"point\":{\"x\":3,\"y\":4},\"tags\":[\"java\",\"kotlin\"]}",
                wrapper.toString()
        );


        sqlClient().getEntities().save(
                JsonWrapperDraft.$.produce(draft -> {
                    draft.setId(1L);
                    draft.setPoint(new Point(4, 3));
                    draft.setTags(Arrays.asList("kotlin", "java"));
                })
        );
        wrapper = sqlClient().getEntities().findById(JsonWrapper.class, 1L);
        Assertions.assertEquals(
                "{\"id\":1,\"point\":{\"x\":4,\"y\":3},\"tags\":[\"kotlin\",\"java\"]}",
                wrapper.toString()
        );

        JsonWrapperTable table = JsonWrapperTable.$;

        sqlClient()
                .createUpdate(table)
                .set(table.tags(), Arrays.asList("java", "kotlin", "scala"))
                .where(table.tags().eq(Arrays.asList("kotlin", "java")))
                .execute();
        wrapper = sqlClient().getEntities().findById(JsonWrapper.class, 1L);
        Assertions.assertEquals(
                "{\"id\":1,\"point\":{\"x\":4,\"y\":3},\"tags\":[\"java\",\"kotlin\",\"scala\"]}",
                wrapper.toString()
        );
    }
}
