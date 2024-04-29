package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.babyfish.jimmer.sql.model.embedded.OrderItemTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class InCollectionTest extends AbstractQueryTest {

    @Test
    public void testSimpleId() {
        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setInListPaddingEnabled(true);
                    cfg.setDialect(new H2Dialect() {
                        @Override
                        public int getMaxInListSize() {
                            return 5;
                        }

                        @Override
                        public boolean isAnyOfArraySupported() {
                            return false;
                        }
                    });
                })
                        .createQuery(table)
                        .where(
                                table.id().in(
                                        Arrays.asList(
                                                1L, 2L, 3L, 4L, 5L,
                                                6L, 7L, 8L
                                        )
                                )
                        )
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where (" +
                                    "--->tb_1_.NODE_ID in (?, ?, ?, ?, ?) " +
                                    "or " +
                                    "--->tb_1_.NODE_ID in (?, ?, ?, ?)" +
                                    ") " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(
                            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L,
                            8L
                    );
                    ctx.rows(
                            "[" +
                                    "{\"id\":1,\"name\":\"Home\",\"parent\":null}," +
                                    "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}," +
                                    "{\"id\":3,\"name\":\"Drinks\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":4,\"name\":\"Coca Cola\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":5,\"name\":\"Fanta\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":6,\"name\":\"Bread\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":7,\"name\":\"Baguette\",\"parent\":{\"id\":6}}," +
                                    "{\"id\":8,\"name\":\"Ciabatta\",\"parent\":{\"id\":6}}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testEmbeddedId() {
        OrderItemTable table = OrderItemTable.$;
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setInListPaddingEnabled(true);
                    cfg.setDialect(new H2Dialect() {
                        @Override
                        public int getMaxInListSize() {
                            return 5;
                        }
                    });
                })
                        .createQuery(table)
                        .where(
                                table.orderId().in(
                                        Arrays.asList(
                                                Objects.createOrderId(id -> id.setX("001").setY("001")),
                                                Objects.createOrderId(id -> id.setX("001").setY("002")),
                                                Objects.createOrderId(id -> id.setX("001").setY("003")),
                                                Objects.createOrderId(id -> id.setX("001").setY("004")),
                                                Objects.createOrderId(id -> id.setX("002").setY("001")),
                                                Objects.createOrderId(id -> id.setX("003").setY("001")),
                                                Objects.createOrderId(id -> id.setX("004").setY("001")),
                                                Objects.createOrderId(id -> id.setX("005").setY("001"))
                                        )
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ORDER_ITEM_A, tb_1_.ORDER_ITEM_B, tb_1_.ORDER_ITEM_C, " +
                                    "tb_1_.NAME, " +
                                    "tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y " +
                                    "from ORDER_ITEM tb_1_ where (" +
                                    "--->(tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) in (" +
                                    "--->--->(?, ?), (?, ?), (?, ?), (?, ?), (?, ?)" +
                                    "--->) " +
                                    "--->or " +
                                    "--->(tb_1_.FK_ORDER_X, tb_1_.FK_ORDER_Y) in (" +
                                    "--->--->(?, ?), (?, ?), (?, ?), (?, ?)" +
                                    "--->)" +
                                    ")"
                    ).variables(
                            "001", "001", "001", "002", "001", "003", "001", "004",
                            "002", "001", "003", "001", "004", "001", "005", "001",

                            "005", "001"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-1-1\"," +
                                    "--->--->\"order\":{\"id\":{\"x\":\"001\",\"y\":\"001\"}}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                                    "--->--->\"name\":\"order-item-1-2\"," +
                                    "--->--->\"order\":{\"id\":{\"x\":\"001\",\"y\":\"001\"}}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":2,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-2-1\"," +
                                    "--->--->\"order\":{\"id\":{\"x\":\"001\",\"y\":\"002\"}}" +
                                    "--->},{" +
                                    "--->--->\"id\":{\"a\":2,\"b\":1,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-2-2\"," +
                                    "--->--->\"order\":{\"id\":{\"x\":\"001\",\"y\":\"002\"}}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testInArrayByH2() {

        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setInListPaddingEnabled(true);
                    cfg.setDialect(new H2Dialect());
                })
                        .createQuery(table)
                        .where(
                                table.id().in(
                                        Arrays.asList(
                                                1L, 2L, 3L, 4L, 5L,
                                                6L, 7L, 8L
                                        )
                                )
                        )
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.NODE_ID = any(?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(
                            (Object) new Object[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L }
                    );
                    ctx.rows(
                            "[" +
                                    "{\"id\":1,\"name\":\"Home\",\"parent\":null}," +
                                    "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}," +
                                    "{\"id\":3,\"name\":\"Drinks\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":4,\"name\":\"Coca Cola\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":5,\"name\":\"Fanta\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":6,\"name\":\"Bread\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":7,\"name\":\"Baguette\",\"parent\":{\"id\":6}}," +
                                    "{\"id\":8,\"name\":\"Ciabatta\",\"parent\":{\"id\":6}}" +
                                    "]"
                    );
                }
        );
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setInListPaddingEnabled(true);
                    cfg.setDialect(new H2Dialect());
                })
                        .createQuery(table)
                        .where(
                                table.parentId().in(
                                        Arrays.asList(1L, 2L, 3L, 4L)
                                )
                        )
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID = any(?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables((Object) new Object[] { 1L, 2L, 3L, 4L });
                    ctx.rows(
                            "[" +
                                    "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}," +
                                    "{\"id\":3,\"name\":\"Drinks\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":4,\"name\":\"Coca Cola\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":5,\"name\":\"Fanta\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":6,\"name\":\"Bread\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":9,\"name\":\"Clothing\",\"parent\":{\"id\":1}}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testInArrayByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg.setInListPaddingEnabled(true);
                    cfg.setDialect(new PostgresDialect());
                })
                        .createQuery(table)
                        .where(
                                table.id().in(
                                        Arrays.asList(
                                                1L, 2L, 3L, 4L, 5L,
                                                6L, 7L, 8L
                                        )
                                )
                        )
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.NODE_ID = any(?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(
                            (Object) new Object[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L }
                    );
                    ctx.rows(
                            "[" +
                                    "{\"id\":1,\"name\":\"Home\",\"parent\":null}," +
                                    "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}," +
                                    "{\"id\":3,\"name\":\"Drinks\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":4,\"name\":\"Coca Cola\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":5,\"name\":\"Fanta\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":6,\"name\":\"Bread\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":7,\"name\":\"Baguette\",\"parent\":{\"id\":6}}," +
                                    "{\"id\":8,\"name\":\"Ciabatta\",\"parent\":{\"id\":6}}" +
                                    "]"
                    );
                }
        );
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setInListPaddingEnabled(true);
                    cfg.setDialect(new H2Dialect());
                })
                        .createQuery(table)
                        .where(
                                table.parentId().in(
                                        Arrays.asList(1L, 2L, 3L, 4L)
                                )
                        )
                        .orderBy(table.id())
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID = any(?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(
                            (Object) new Object[] { 1L, 2L, 3L, 4L }
                    );
                    ctx.rows(
                            "[" +
                                    "{\"id\":2,\"name\":\"Food\",\"parent\":{\"id\":1}}," +
                                    "{\"id\":3,\"name\":\"Drinks\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":4,\"name\":\"Coca Cola\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":5,\"name\":\"Fanta\",\"parent\":{\"id\":3}}," +
                                    "{\"id\":6,\"name\":\"Bread\",\"parent\":{\"id\":2}}," +
                                    "{\"id\":9,\"name\":\"Clothing\",\"parent\":{\"id\":1}}" +
                                    "]"
                    );
                }
        );
    }
}
