package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

public class SQLiteDeleteTest extends AbstractMutationTest {
    @BeforeAll
    public static void beforeAll() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));
    }

    @Test
    public void deleteTreeByDepth2() {
        executeAndExpectResult(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new SQLiteDialect());
                    it.setMaxCommandJoinCount(2);
                }).getEntities().deleteCommand(
                        TreeNode.class,
                        1L
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID = ?"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "inner join TREE_NODE tb_2_ on tb_1_.PARENT_ID = tb_2_.NODE_ID " +
                                        "inner join TREE_NODE tb_3_ on tb_2_.PARENT_ID = tb_3_.NODE_ID " +
                                        "where tb_3_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->TREE_NODE.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where NODE_ID in (?, ?, ?, ?, ?, ?, ?, ?)"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "delete from TREE_NODE " +
                                        "where exists(" +
                                        "--->select * " +
                                        "--->from TREE_NODE tb_2_ " +
                                        "--->where " +
                                        "--->--->TREE_NODE.PARENT_ID = tb_2_.NODE_ID " +
                                        "--->and " +
                                        "--->--->tb_2_.PARENT_ID = ?" +
                                        ")"
                        );
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where PARENT_ID = ?");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from TREE_NODE where NODE_ID = ?");
                    });
                    ctx.totalRowCount(24);
                }
        );
    }
}
