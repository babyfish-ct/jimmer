package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.model.Objects;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.babyfish.jimmer.sql.model.embedded.MachineFetcher;
import org.babyfish.jimmer.sql.model.embedded.MachineTable;
import org.babyfish.jimmer.sql.model.embedded.OrderItemTable;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorMetadataTable;
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
                        public boolean isAnyEqualityOfArraySupported() {
                            return false;
                        }
                        @Override
                        public int getMaxInListSize() {
                            return 5;
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
    public void testEmbeddedPathOfBug596() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.location().host().in(
                                        Arrays.asList(
                                                "localhost",
                                                "127.0.0.1"
                                        )
                                )
                        )
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from MACHINE tb_1_ " +
                                    "where tb_1_.HOST = any(?)"
                    );
                }
        );
    }

    @Test
    public void testEmbeddedShallowPathOfBug596() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.source().leftTop().in(
                                        Arrays.asList(
                                                Objects.createPoint(point -> point.setX(100).setY(120)),
                                                Objects.createPoint(point -> point.setX(150).setY(170))
                                        )
                                )
                        )
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from TRANSFORM tb_1_ " +
                                    "where (tb_1_.`LEFT`, tb_1_.TOP) in ((?, ?), (?, ?))"
                    );
                }
        );
    }

    @Test
    public void testInArrayByH2() {

        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setDialect(new H2Dialect());
                    cfg.setInListToAnyEqualityEnabled(true);
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
                    cfg.setInListToAnyEqualityEnabled(true);
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
                    cfg.setDialect(new PostgresDialect());
                    cfg.setInListToAnyEqualityEnabled(true);
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
                    cfg.setInListToAnyEqualityEnabled(true);
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

    @Test
    public void testEnumInArrayByPostgres() {
        NativeDatabases.assumeNativeDatabase();

        AuthorTable table = AuthorTable.$;
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg.setDialect(new PostgresDialect());
                    cfg.setInListToAnyEqualityEnabled(true);
                })
                        .createQuery(table)
                        .where(
                                table.gender().in(
                                        Arrays.asList(Gender.MALE, Gender.FEMALE)
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR tb_1_ " +
                                    "where tb_1_.GENDER = any(?)"
                    ).variables(
                            (Object) new Object[] { "M", "F" }
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->\"firstName\":\"Eve\"," +
                                    "--->--->\"lastName\":\"Procello\"," +
                                    "--->--->\"gender\":\"FEMALE\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->\"firstName\":\"Alex\"," +
                                    "--->--->\"lastName\":\"Banks\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"c14665c8-c689-4ac7-b8cc-6f065b8d835d\"," +
                                    "--->--->\"firstName\":\"Dan\"," +
                                    "--->--->\"lastName\":\"Vanderkam\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"," +
                                    "--->--->\"firstName\":\"Boris\"," +
                                    "--->--->\"lastName\":\"Cherny\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->},{" +
                                    "--->--->\"id\":\"eb4963fd-5223-43e8-b06b-81e6172ee7ae\"," +
                                    "--->--->\"firstName\":\"Samer\"," +
                                    "--->--->\"lastName\":\"Buna\"," +
                                    "--->--->\"gender\":\"MALE\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testNullableInByTuple() {

        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setDialect(new H2Dialect() {
                        @Override
                        public int getMaxInListSize() {
                            return 5;
                        }
                    });
                })
                        .createQuery(table)
                        .where(
                                Expression.tuple(
                                        table.parentId(),
                                        table.name()
                                ).nullableIn(
                                        Arrays.asList(
                                                new Tuple2<>(1L, "Food"),
                                                new Tuple2<>(1L, "Cloth"),
                                                new Tuple2<>(2L, "Drinks"),
                                                new Tuple2<>(2L, "Bread"),
                                                new Tuple2<>(null, "Home"),
                                                new Tuple2<>(3L, "Cococola"),
                                                new Tuple2<>(3L, "Fenta"),
                                                new Tuple2<>(null, "Cococola"),
                                                new Tuple2<>(null, "Fenta")
                                        )
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where (" +
                                    "--->--->(tb_1_.PARENT_ID, tb_1_.NAME) in ((?, ?), (?, ?), (?, ?), (?, ?), (?, ?)) " +
                                    "--->or " +
                                    "--->--->(tb_1_.PARENT_ID, tb_1_.NAME) in ((?, ?)) " +
                                    "--->or tb_1_.PARENT_ID is null and tb_1_.NAME = any(?)" +
                                    ")"
                    ).variables(
                            1L, "Food", 1L, "Cloth", 2L, "Drinks", 2L, "Bread", 3L, "Cococola",
                            3L, "Fenta",
                            new Object[] { "Home", "Cococola", "Fenta"}
                    );
                }
        );
    }

    @Test
    public void testNullableInWithPaddingByTuple() {

        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient(cfg -> {
                    cfg.setDialect(new H2Dialect() {
                        @Override
                        public int getMaxInListSize() {
                            return 5;
                        }
                    });
                    cfg.setInListPaddingEnabled(true);
                    cfg.setExpandedInListPaddingEnabled(true);
                })
                        .createQuery(table)
                        .where(
                                Expression.tuple(
                                        table.parentId(),
                                        table.name()
                                ).nullableIn(
                                        Arrays.asList(
                                                new Tuple2<>(1L, "Food"),
                                                new Tuple2<>(1L, "Cloth"),
                                                new Tuple2<>(2L, "Drinks"),
                                                new Tuple2<>(2L, "Bread"),
                                                new Tuple2<>(null, "Home"),
                                                new Tuple2<>(3L, "Cococola"),
                                                new Tuple2<>(3L, "Fenta"),
                                                new Tuple2<>(4L, "Cococola"),
                                                new Tuple2<>(5L, "Fenta"),
                                                new Tuple2<>(null, "Cococola"),
                                                new Tuple2<>(null, "Fenta")
                                        )
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where (" +
                                    "--->(tb_1_.PARENT_ID, tb_1_.NAME) in (" +
                                    "--->--->(?, ?), (?, ?), (?, ?), (?, ?), (?, ?)" +
                                    "--->) " +
                                    "--->or " +
                                    "--->(tb_1_.PARENT_ID, tb_1_.NAME) in (" +
                                    "--->--->(?, ?), (?, ?), (?, ?), (?, ?)" +
                                    "--->) " +
                                    "--->or " +
                                    "--->tb_1_.PARENT_ID is null and tb_1_.NAME = any(?))"
                    ).variables(
                            1L, "Food", 1L, "Cloth", 2L, "Drinks", 2L, "Bread", 3L, "Cococola",
                            3L, "Fenta", 4L, "Cococola", 5L, "Fenta",
                            5L, "Fenta", // repeated data by `jimmer.in-list-padding-enabled`
                            new Object[]{ "Home", "Cococola", "Fenta" }
                    );
                }
        );
    }

    @Test
    public void testNullableInByEmbedded() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.location().nullableIn(
                                        Arrays.asList(
                                                Objects.createLocation(l -> l.setHost("localhost").setPort(80)),
                                                Objects.createLocation(l -> l.setHost("localhost").setPort(443)),
                                                Objects.createLocation(l -> l.setHost("localhost").setPort(null)),
                                                Objects.createLocation(l -> l.setHost("127.0.0.1").setPort(80)),
                                                Objects.createLocation(l -> l.setHost("127.0.0.1").setPort(443)),
                                                Objects.createLocation(l -> l.setHost("127.0.0.1").setPort(null))
                                        )
                                )
                        )
                        .select(
                                table.fetch(
                                        MachineFetcher.$
                                                .location()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.HOST, tb_1_.PORT " +
                                    "from MACHINE tb_1_ " +
                                    "where (" +
                                    "--->(tb_1_.HOST, tb_1_.PORT) in ((?, ?), (?, ?), (?, ?), (?, ?)) " +
                                    "or " +
                                    "--->tb_1_.PORT is null " +
                                    "and " +
                                    "--->tb_1_.HOST = any(?)" +
                                    ")"
                    ).variables(
                            "localhost",
                            80,
                            "localhost",
                            443,
                            "127.0.0.1",
                            80,
                            "127.0.0.1",
                            443,
                            new Object[]{"localhost", "127.0.0.1"}
                    );
                }
        );
    }

    @Test
    public void testNullableInArrayByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(cfg -> {
                    cfg.setDialect(new PostgresDialect());
                    cfg.setInListToAnyEqualityEnabled(true);
                })
                        .createQuery(table)
                        .where(
                                table.parentId().nullableIn(
                                        Arrays.asList(null, 1L, 2L, 3L)
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where (" +
                                    "--->tb_1_.PARENT_ID = any(?) " +
                                    "or " +
                                    "--->tb_1_.PARENT_ID is null" +
                                    ")"
                    ).variables(
                            (Object) new Object[] { 1L, 2L, 3L }
                    );
                }
        );
    }

    @Test
    public void testNullableNotIn() {
        MachineTable table = MachineTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.location().nullableNotIn(
                                        Arrays.asList(
                                                Objects.createLocation(l -> l.setHost("localhost").setPort(80)),
                                                Objects.createLocation(l -> l.setHost("localhost").setPort(443)),
                                                Objects.createLocation(l -> l.setHost("localhost").setPort(null)),
                                                Objects.createLocation(l -> l.setHost("127.0.0.1").setPort(80)),
                                                Objects.createLocation(l -> l.setHost("127.0.0.1").setPort(443)),
                                                Objects.createLocation(l -> l.setHost("127.0.0.1").setPort(null))
                                        )
                                )
                        )
                        .select(
                                table.fetch(
                                        MachineFetcher.$
                                                .location()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.HOST, tb_1_.PORT " +
                                    "from MACHINE tb_1_ " +
                                    "where " +
                                    "--->(tb_1_.HOST, tb_1_.PORT) not in ((?, ?), (?, ?), (?, ?), (?, ?)) " +
                                    "and " +
                                    "(tb_1_.PORT is not null or not (tb_1_.HOST = any(?)))"
                    ).variables(
                            "localhost",
                            80,
                            "localhost",
                            443,
                            "127.0.0.1",
                            80,
                            "127.0.0.1",
                            443,
                            new Object[] { "localhost", "127.0.0.1" }
                    );
                }
        );
    }

    @Test
    public void testMixedEmbeddedShape() {
        TransformTable table = TransformTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.source().in(
                                        Arrays.asList(
                                                Objects.createRect(rect -> {
                                                    rect.leftTop(true).setX(150);
                                                    rect.rightBottom(true).setY(370);
                                                }),
                                                Objects.createRect(rect -> {
                                                    rect.leftTop(true).setX(150);
                                                    rect.rightBottom(true).setY(371);
                                                }),
                                                Objects.createRect(rect -> {
                                                    rect.leftTop(true).setY(120);
                                                    rect.rightBottom(true).setX(400);
                                                }),
                                                Objects.createRect(rect -> {
                                                    rect.leftTop(true).setY(120);
                                                    rect.rightBottom(true).setX(401);
                                                })
                                        )
                                )
                        )
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, " +
                                    "tb_1_.`LEFT`, tb_1_.TOP, tb_1_.`RIGHT`, tb_1_.BOTTOM, " +
                                    "tb_1_.TARGET_LEFT, tb_1_.TARGET_TOP, tb_1_.TARGET_RIGHT, tb_1_.TARGET_BOTTOM " +
                                    "from TRANSFORM tb_1_ " +
                                    "where (" +
                                    "--->(tb_1_.`LEFT`, tb_1_.BOTTOM) in ((?, ?), (?, ?)) " +
                                    "--->or " +
                                    "--->(tb_1_.TOP, tb_1_.`RIGHT`) in ((?, ?), (?, ?))" +
                                    ")"
                    ).variables(
                            150L, 370L, 150L, 371L, 120L, 400L, 120L, 401L
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":1," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":100,\"y\":120}," +
                                    "--->--->--->\"rightBottom\":{\"x\":400,\"y\":320}" +
                                    "--->--->}," +
                                    "--->--->\"target\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":800,\"y\":600}," +
                                    "--->--->--->\"rightBottom\":{\"x\":1400,\"y\":1000}" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"id\":2," +
                                    "--->--->\"source\":{" +
                                    "--->--->--->\"leftTop\":{\"x\":150,\"y\":170}," +
                                    "--->--->--->\"rightBottom\":{\"x\":450,\"y\":370}" +
                                    "--->--->}," +
                                    "--->--->\"target\":null" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testIssue598() {
        AdministratorMetadataTable table = AdministratorMetadataTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(
                                table.administrator().asTableEx().roles().id().in(
                                        Arrays.asList(100L, 200L)
                                )
                        )
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from ADMINISTRATOR_METADATA tb_1_ " +
                                    "inner join ADMINISTRATOR tb_2_ on tb_1_.ADMINISTRATOR_ID = tb_2_.ID " +
                                    "inner join ADMINISTRATOR_ROLE_MAPPING tb_3_ on tb_2_.ID = tb_3_.ADMINISTRATOR_ID " +
                                    "inner join ROLE tb_4_ on tb_3_.ROLE_ID = tb_4_.ID " +
                                    "where tb_4_.ID = any(?) and " +
                                    "tb_1_.DELETED <> ? and tb_4_.DELETED <> ? and tb_2_.DELETED <> ?"
                    );
                    ctx.rows("[10,30]");
                }
        );
    }

    @Override
    protected boolean isAnyEqualityOfArraySupported() {
        return true;
    }
}
