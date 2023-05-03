package org.babyfish.jimmer.sql.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple4;
import org.babyfish.jimmer.sql.ast.tuple.Tuple5;
import org.babyfish.jimmer.sql.model.pg.JsonWrapper;
import org.babyfish.jimmer.sql.model.pg.JsonWrapperDraft;
import org.babyfish.jimmer.sql.model.pg.JsonWrapperTable;
import org.babyfish.jimmer.sql.model.pg.Point;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScalarProviderTest extends AbstractJsonTest {

    @Test
    public void test() {
        sqlClient().getEntities().save(
                JsonWrapperDraft.$.produce(draft -> {
                    draft.setId(1L);
                    draft.setPoint(new Point(3, 4));
                    draft.setTags(Arrays.asList("java", "kotlin"));
                    draft.setScores(Collections.singletonMap(1L, 100));
                    draft.setComplexList(
                            Arrays.asList(
                                    Arrays.asList("1-1", "1-2"),
                                    Arrays.asList("2-1", "2-2")
                            )
                    );
                    draft.setComplexMap(
                            Collections.singletonMap(
                                    "key",
                                    Collections.singletonMap("nested-key", "value")
                            )
                    );
                })
        );
        JsonWrapper wrapper = sqlClient().getEntities().findById(JsonWrapper.class, 1L);
        Assertions.assertEquals(
                "{" +
                        "\"id\":1," +
                        "\"point\":{\"_x\":3,\"_y\":4}," +
                        "\"tags\":[\"java\",\"kotlin\"]," +
                        "\"scores\":{\"1\":100}," +
                        "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                        "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                        "}",
                wrapper.toString()
        );

        JsonWrapperTable table = JsonWrapperTable.$;

        Tuple5<Point, List<String>, Map<Long, Integer>, List<List<String>>, Map<String, Map<String, String>>> tuple =
                sqlClient()
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.point(),
                                table.tags(),
                                table.scores(),
                                table.complexList(),
                                table.complexMap()
                        )
                        .fetchOne();
        Assertions.assertEquals(
                "Tuple5(" +
                        "_1=Point{x=3, y=4}, " +
                        "_2=[java, kotlin], " +
                        "_3={1=100}, " +
                        "_4=[[1-1, 1-2], [2-1, 2-2]], " +
                        "_5={key={nested-key=value}}" +
                        ")",
                tuple.toString()
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
                "{" +
                        "\"id\":1," +
                        "\"point\":{\"_x\":4,\"_y\":3}," +
                        "\"tags\":[\"kotlin\",\"java\"]," +
                        "\"scores\":{\"1\":100}," +
                        "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                        "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                        "}",
                wrapper.toString()
        );

        sqlClient()
                .createUpdate(table)
                .set(table.tags(), Arrays.asList("java", "kotlin", "scala"))
                .where(table.tags().eq(Arrays.asList("kotlin", "java")))
                .execute();
        wrapper = sqlClient().getEntities().findById(JsonWrapper.class, 1L);
        Assertions.assertEquals(
                "{" +
                        "\"id\":1," +
                        "\"point\":{\"_x\":4,\"_y\":3}," +
                        "\"tags\":[\"java\",\"kotlin\",\"scala\"]," +
                        "\"scores\":{\"1\":100}," +
                        "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]]," +
                        "\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                        "}",
                wrapper.toString()
        );

        sqlClient()
                .createUpdate(table)
                .set(table.scores(), Collections.singletonMap(2L, 200))
                .where(
                        Expression.tuple(table.tags(), table.scores()).eq(
                                new Tuple2<>(
                                        Arrays.asList("java", "kotlin", "scala"),
                                        Collections.singletonMap(1L, 100)
                                )
                        )
                )
                .execute();
        wrapper = sqlClient().getEntities().findById(JsonWrapper.class, 1L);
        Assertions.assertEquals(
                "{" +
                        "\"id\":1," +
                        "\"point\":{\"_x\":4,\"_y\":3}," +
                        "\"tags\":[\"java\",\"kotlin\",\"scala\"]," +
                        "\"scores\":{\"2\":200}," +
                        "\"complexList\":[[\"1-1\",\"1-2\"],[\"2-1\",\"2-2\"]],\"complexMap\":{\"key\":{\"nested-key\":\"value\"}}" +
                        "}",
                wrapper.toString()
        );
    }
}
