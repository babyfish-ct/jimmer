package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.impl.mutation.save.QueryReason;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.junit.jupiter.api.Test;

public class DatabaseAutoIdUpsertTest extends AbstractMutationTest {

    @Test
    public void testSequenceByH2() {
        executeAndExpectResult(
                getSqlClient(
                        it -> it.setDialect(new H2Dialect())
                ).getEntities().saveCommand(
                        TreeNodeDraft.$.produce(treeNode -> {
                            treeNode.setName("Computer");
                            treeNode.setParent(null);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID is null and tb_1_.NAME = ?"
                        );
                        it.variables("Computer");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("select nextval('tree_node_id_seq')");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) values(?, ?, ?)");
                        it.variables(100L, "Computer", new DbLiteral.DbNull(long.class));
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":100,\"name\":\"Computer\",\"parent\":null}");
                    });
                }
        );
    }

    @Test
    public void testSequenceByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        jdbc(NativeDatabases.POSTGRES_DATA_SOURCE, false, con -> {
            con
                    .createStatement()
                    .executeUpdate("alter sequence tree_node_id_seq restart with 100");
        });

        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new PostgresDialect())
                ).getEntities().saveCommand(
                        TreeNodeDraft.$.produce(treeNode -> {
                            treeNode.setName("Computer");
                            treeNode.setParent(null);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                        "from TREE_NODE tb_1_ " +
                                        "where tb_1_.PARENT_ID is null and tb_1_.NAME = ?"
                        );
                        it.variables("Computer");
                        it.queryReason(QueryReason.IDENTITY_GENERATOR_REQUIRED);
                    });
                    ctx.statement(it -> {
                        it.sql("select nextval('tree_node_id_seq')");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME, PARENT_ID) values(?, ?, ?)");
                        it.variables(100L, "Computer", new DbLiteral.DbNull(long.class));
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":100,\"name\":\"Computer\",\"parent\":null}");
                    });
                }
        );
    }

    @Test
    public void testIdentityByMySql() {

        NativeDatabases.assumeNativeDatabase();

        jdbc(NativeDatabases.MYSQL_DATA_SOURCE, false, con -> {
            con
                    .createStatement()
                    .executeUpdate("alter table tree_node auto_increment = 100");
        });

        executeAndExpectResult(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(
                        it -> it
                                .setDialect(new MySqlDialect())
                                .setIdGenerator(TreeNode.class, IdentityIdGenerator.INSTANCE)
                ).getEntities().saveCommand(
                        TreeNodeDraft.$.produce(treeNode -> {
                            treeNode.setName("Computer");
                            treeNode.setParent(null);
                        })
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert ignore into TREE_NODE(NAME, PARENT_ID) values(?, ?)");
                        it.variables("Computer", new DbLiteral.DbNull(long.class));
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":100,\"name\":\"Computer\",\"parent\":null}");
                    });
                }
        );
    }
}
