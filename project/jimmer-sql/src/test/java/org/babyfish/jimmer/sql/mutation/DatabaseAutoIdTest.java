package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.junit.jupiter.api.Test;

public class DatabaseAutoIdTest extends AbstractMutationTest {

    @Test
    public void testSequenceByH2() {
        executeAndExpectResult(
                getSqlClient(
                        it -> it.setDialect(new H2Dialect())
                ).getEntities().saveCommand(
                        TreeNodeDraft.$.produce(treeNode -> {
                            treeNode.setName("Computer");
                        })
                ).configure(it -> it.setMode(SaveMode.INSERT_ONLY)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select nextval('tree_node_id_seq')");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME) values(?, ?)");
                        it.variables(100L, "Computer");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":100,\"name\":\"Computer\"}");
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
                        })
                ).configure(it -> it.setMode(SaveMode.INSERT_ONLY)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("select nextval('tree_node_id_seq')");
                    });
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NODE_ID, NAME) values(?, ?)");
                        it.variables(100L, "Computer");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":100,\"name\":\"Computer\"}");
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
                        })
                ).configure(it -> it.setMode(SaveMode.INSERT_ONLY)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into TREE_NODE(NAME) values(?)");
                        it.variables("Computer");
                    });
                    ctx.entity(it -> {
                        it.modified("{\"id\":100,\"name\":\"Computer\"}");
                    });
                }
        );
    }
}
