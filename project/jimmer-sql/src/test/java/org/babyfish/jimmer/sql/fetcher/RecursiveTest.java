package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.junit.jupiter.api.Test;

public class RecursiveTest extends AbstractQueryTest {

    @Test
    public void testFindTwoLevel() {
        executeAndExpect(
                getSqlClient().createQuery(TreeNodeTable.class, (q, node) -> {
                    q.where(node.parent().isNull());
                    return q.select(
                            node.fetch(
                                    TreeNodeFetcher.$.name().childNodes(
                                            TreeNodeFetcher.$.name(),
                                            it -> it.batch(2).depth(2)
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID is null");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?)"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }

    @Test
    public void testFindThreeLevel() {
        executeAndExpect(
                getSqlClient().createQuery(TreeNodeTable.class, (q, node) -> {
                    q.where(node.parent().isNull());
                    return q.select(
                            node.fetch(
                                    TreeNodeFetcher.$.name().childNodes(
                                            TreeNodeFetcher.$.name(),
                                            it -> it.batch(2).depth(3)
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID is null");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?)"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }

    @Test
    public void testFindUnlimitedLevel() {
        executeAndExpect(
                getSqlClient().createQuery(TreeNodeTable.class, (q, node) -> {
                    q.where(node.parent().isNull());
                    return q.select(
                            node.fetch(
                                    TreeNodeFetcher.$.name().childNodes(
                                            TreeNodeFetcher.$.name(),
                                            it -> it.batch(2).recursive()
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID is null");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?)"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.statement(4).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }

    @Test
    public void testFindDynamically() {
        executeAndExpect(
                getSqlClient().createQuery(TreeNodeTable.class, (q, node) -> {
                    q.where(node.parent().isNull());
                    return q.select(
                            node.fetch(
                                    TreeNodeFetcher.$.name().childNodes(
                                            TreeNodeFetcher.$.name(),
                                            it -> it
                                                    .batch(2)
                                                    .recursive(
                                                            (n, depth) -> !n.name().equals("Drinks")
                                                    )
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID is null");
                    ctx.statement(1).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?)"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.statement(4).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID in (?, ?)"
                    );
                    ctx.rows(System.out::println);
                }
        );
    }
}
