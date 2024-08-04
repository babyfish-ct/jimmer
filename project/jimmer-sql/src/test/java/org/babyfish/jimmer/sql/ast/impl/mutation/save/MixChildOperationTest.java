package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeProps;
import org.babyfish.jimmer.sql.model.flat.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MixChildOperationTest extends AbstractChildOperatorTest {

    @Test
    public void testDisconnectTreeExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(4)),
                            con,
                            ProvinceProps.COUNTRY.unwrap()
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>("China", 2L),
                                    new Tuple2<>("USA", 5L)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update COMPANY tb_1_ " +
                                        "set STREET_ID = null " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from STREET tb_2_ " +
                                        "--->inner join CITY tb_3_ on tb_2_.CITY_ID = tb_3_.ID " +
                                        "--->inner join PROVINCE tb_4_ on tb_3_.PROVINCE_ID = tb_4_.ID " +
                                        "--->where " +
                                        "--->--->tb_1_.STREET_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_4_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_4_.COUNTRY_ID, tb_4_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from STREET tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from CITY tb_2_ " +
                                        "--->inner join PROVINCE tb_3_ on tb_2_.PROVINCE_ID = tb_3_.ID " +
                                        "--->where " +
                                        "--->--->tb_1_.CITY_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_3_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_3_.COUNTRY_ID, tb_3_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from PROVINCE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PROVINCE_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_2_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_2_.COUNTRY_ID, tb_2_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE " +
                                        "where COUNTRY_ID in (?, ?) and (COUNTRY_ID, ID) not in ((?, ?), (?, ?))"
                        );
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(3, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(Street.class)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(City.class)));
                        Assertions.assertEquals(4, map.get(AffectedTable.of(Province.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectTreeWithShallowSubQueryDepthExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(2)),
                            con,
                            ProvinceProps.COUNTRY.unwrap()
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>("China", 2L),
                                    new Tuple2<>("USA", 5L)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from STREET tb_1_ " +
                                        "inner join CITY tb_2_ on tb_1_.CITY_ID = tb_2_.ID " +
                                        "inner join PROVINCE tb_3_ on tb_2_.PROVINCE_ID = tb_3_.ID " +
                                        "where " +
                                        "--->tb_3_.COUNTRY_ID in (?, ?) " +
                                        "and " +
                                        "--->(tb_3_.COUNTRY_ID, tb_2_.PROVINCE_ID) not in ((?, ?), (?, ?))"
                        );
                        it.variables("China", "USA", "China", 2L, "USA", 5L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "update COMPANY " +
                                        "set STREET_ID = null " +
                                        "where STREET_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                1L, 2L, 3L, 4L, 9L, 10L, 11L, 12L,
                                13L, 14L, 15L, 16L, 21L, 22L, 23L, 24L
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from STREET " +
                                        "where ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                1L, 2L, 3L, 4L, 9L, 10L, 11L, 12L,
                                13L, 14L, 15L, 16L, 21L, 22L, 23L, 24L
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from PROVINCE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PROVINCE_ID = tb_2_.ID " +
                                        "--->and " +
                                        "--->--->tb_2_.COUNTRY_ID in (?, ?) " +
                                        "--->and " +
                                        "--->--->(tb_2_.COUNTRY_ID, tb_2_.ID) not in ((?, ?), (?, ?))" +
                                        ")"
                        );
                        it.variables("China", "USA", "China", 2L, "USA", 5L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE " +
                                        "where " +
                                        "--->COUNTRY_ID in (?, ?) " +
                                        "and " +
                                        "--->(COUNTRY_ID, ID) not in ((?, ?), (?, ?)" +
                                        ")"
                        );
                        it.variables("China", "USA", "China", 2L, "USA", 5L);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(3, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(Street.class)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(City.class)));
                        Assertions.assertEquals(4, map.get(AffectedTable.of(Province.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectTreeWithShallowestSubQueryDepthExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(0)),
                            con,
                            ProvinceProps.COUNTRY.unwrap()
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>("China", 2L),
                                    new Tuple2<>("USA", 5L)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from PROVINCE tb_1_ " +
                                        "where " +
                                        "--->tb_1_.COUNTRY_ID in (?, ?) " +
                                        "and " +
                                        "--->(tb_1_.COUNTRY_ID, tb_1_.ID) not in ((?, ?), (?, ?))"
                        );
                        it.variables("China", "USA", "China", 2L, "USA", 5L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from CITY tb_1_ " +
                                        "where tb_1_.PROVINCE_ID in (?, ?, ?, ?)"
                        );
                        it.variables(1L, 3L, 4L, 6L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from STREET tb_1_ " +
                                        "where tb_1_.CITY_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                1L, 2L, 5L, 6L, 7L, 8L, 11L, 12L
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID " +
                                        "from COMPANY tb_1_ " +
                                        "where tb_1_.STREET_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                1L, 2L, 3L, 4L, 9L, 10L, 11L, 12L,
                                13L, 14L, 15L, 16L, 21L, 22L, 23L, 24L
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from STREET " +
                                        "where ID in (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(
                                1L, 2L, 3L, 4L, 9L, 10L, 11L, 12L,
                                13L, 14L, 15L, 16L, 21L, 22L, 23L, 24L
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from CITY " +
                                        "where ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(1L, 2L, 5L, 6L, 7L, 8L, 11L, 12L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from PROVINCE where ID in (?, ?, ?, ?)"
                        );
                        it.variables(1L, 3L, 4L, 6L);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(3, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(Street.class)));
                        Assertions.assertEquals(8, map.get(AffectedTable.of(City.class)));
                        Assertions.assertEquals(4, map.get(AffectedTable.of(Province.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectRecursiveTreeExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(4)),
                            con,
                            TreeNodeProps.PARENT.unwrap()
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "inner join TREE_NODE tb_4_ on tb_3_.PARENT_ID = tb_4_.NODE_ID " +
                                        "inner join TREE_NODE tb_5_ on tb_4_.PARENT_ID = tb_5_.NODE_ID " +
                                        "where tb_5_.PARENT_ID = ? and tb_4_.PARENT_ID <> ?"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "--->inner join TREE_NODE tb_4_ on tb_3_.PARENT_ID = tb_4_.NODE_ID " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_4_.PARENT_ID = ? and tb_4_.NODE_ID <> ?" +
                                        ")"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_3_.PARENT_ID = ? and tb_3_.NODE_ID <> ?" +
                                        ")"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID = ? and tb_2_.NODE_ID <> ?" +
                                        ")"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where PARENT_ID = ? and NODE_ID <> ?"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(TreeNode.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectRecursiveTreeWithSallowSubQueryExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(2)),
                            con,
                            TreeNodeProps.PARENT.unwrap()
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = ? and tb_2_.PARENT_ID <> ?"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID in (?, ?, ?, ?)"
                        );
                        it.variables(11L, 15L, 19L, 22L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID in (?, ?, ?, ?)" +
                                        ")"
                        );
                        it.variables(11L, 15L, 19L, 22L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where PARENT_ID in (?, ?, ?, ?)"
                        );
                        it.variables(11L, 15L, 19L, 22L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?)"
                        );
                        it.variables(11L, 15L, 19L, 22L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE tb_1_ " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID = ? and tb_2_.NODE_ID <> ?" +
                                        ")"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE where PARENT_ID = ? and NODE_ID <> ?"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(TreeNode.class)));
                    });
                }
        );
    }

    @Test
    public void testDisconnectRecursiveTreeWithSallowestSubQueryExcept() {
        connectAndExpect(
                con -> {
                    ChildTableOperator operator = operator(
                            getSqlClient(it -> it.setMaxCommandJoinCount(0)),
                            con,
                            TreeNodeProps.PARENT.unwrap()
                    );
                    operator.disconnectExcept(
                            IdPairs.of(
                                    new Tuple2<>(1L, 2L)
                            )
                    );
                    return operator.ctx.affectedRowCountMap;
                },
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID = ? and tb_1_.NODE_ID <> ?"
                        );
                        it.variables(1L, 2L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID = ?"
                        );
                        it.variables(9L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (?, ?)"
                        );
                        it.variables(10L, 18L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (?, ?, ?, ?)"
                        );
                        it.variables(11L, 15L, 19L, 22L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID in (" +
                                        "--->?, ?, ?, ?, ?, ?, ?, ?, ?" +
                                        ")"
                        );
                        it.variables(12L, 13L, 14L, 16L, 17L, 20L, 21L, 23L, 24L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                        it.variables(12L, 13L, 14L, 16L, 17L, 20L, 21L, 23L, 24L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?)"
                        );
                        it.variables(11L, 15L, 19L, 22L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?)"
                        );
                        it.variables(10L, 18L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE where NODE_ID = ?"
                        );
                        it.variables(9L);
                    });
                    ctx.value(map -> {
                        Assertions.assertEquals(1, map.size());
                        Assertions.assertEquals(16, map.get(AffectedTable.of(TreeNode.class)));
                    });
                }
        );
    }
}
