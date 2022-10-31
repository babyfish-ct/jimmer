package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.TreeNodeFetcher;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.junit.jupiter.api.Test;

/*
 * Mix recursive fetcher and multi level fetcher
 *
 * Equality of Filter is very important, so
 * Filter.sortingFilter(TreeNodeTable.class, TreeNodeTable::id)
 * is used here
 */
public class MixedTest extends AbstractQueryTest {

    @Test
    public void test() {
        executeAndExpect(
                getLambdaClient().createQuery(TreeNodeTable.class, (q, node) -> {
                    q.where(node.parent().isNull());
                    return q.select(
                            node.fetch(
                                    TreeNodeFetcher.$.name().childNodes(
                                            TreeNodeFetcher.$.name().childNodes(
                                                    TreeNodeFetcher.$.name().childNodes(
                                                            TreeNodeFetcher.$.name(),
                                                            it -> it.recursive()
                                                    ),
                                                    it -> it.recursive()
                                            ),
                                            it -> it.recursive()
                                    )
                            )
                    );
                }),
                ctx -> {
                    ctx.sql("select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE as tb_1_ where tb_1_.PARENT_ID is null");
                    ctx.statement(1).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID = ? " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(1L);
                    ctx.statement(2).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(2L, 9L);
                    ctx.statement(3).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(3L, 6L, 10L, 18L);
                    ctx.statement(4).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(4L, 5L, 7L, 8L, 11L, 15L, 19L, 22L);
                    ctx.statement(5).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE as tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(12L, 13L, 14L, 16L, 17L, 20L, 21L, 23L, 24L);
                    ctx.rows("[{" +
                            "--->\"id\":1," +
                            "--->\"name\":" +
                            "--->\"Home\"," +
                            "--->\"childNodes\":[" +
                            "--->--->{" +
                            "--->--->--->\"id\":2," +
                            "--->--->--->\"name\":\"Food\"," +
                            "--->--->--->\"childNodes\":[" +
                            "--->--->--->--->{" +
                            "--->--->--->--->--->\"id\":3," +
                            "--->--->--->--->--->\"name\":\"Drinks\"," +
                            "--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->{\"id\":4,\"name\":\"Coca Cola\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->{\"id\":5,\"name\":\"Fanta\",\"childNodes\":[]}" +
                            "--->--->--->--->--->]" +
                            "--->--->--->--->},{" +
                            "--->--->--->--->--->\"id\":6," +
                            "--->--->--->--->--->\"name\":\"Bread\"," +
                            "--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->{\"id\":7,\"name\":\"Baguette\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->{\"id\":8,\"name\":\"Ciabatta\",\"childNodes\":[]}" +
                            "--->--->--->--->--->]" +
                            "--->--->--->--->}" +
                            "--->--->--->]" +
                            "--->--->},{" +
                            "--->--->--->\"id\":9," +
                            "--->--->--->\"name\":\"Clothing\"," +
                            "--->--->--->\"childNodes\":[" +
                            "--->--->--->--->{" +
                            "--->--->--->--->--->\"id\":10," +
                            "--->--->--->--->--->\"name\":\"Woman\"," +
                            "--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->\"id\":11," +
                            "--->--->--->--->--->--->--->\"name\":\"Casual wear\"," +
                            "--->--->--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->--->--->{\"id\":12,\"name\":\"Dress\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->--->--->{\"id\":13,\"name\":\"Miniskirt\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->--->--->{\"id\":14,\"name\":\"Jeans\",\"childNodes\":[]}" +
                            "--->--->--->--->--->--->--->]" +
                            "--->--->--->--->--->--->},{" +
                            "--->--->--->--->--->--->--->\"id\":15," +
                            "--->--->--->--->--->--->--->\"name\":\"Formal wear\"," +
                            "--->--->--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->--->--->{\"id\":16,\"name\":\"Suit\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->--->--->{\"id\":17,\"name\":\"Shirt\",\"childNodes\":[]}" +
                            "--->--->--->--->--->--->--->]" +
                            "--->--->--->--->--->--->}" +
                            "--->--->--->--->--->]" +
                            "--->--->--->--->},{" +
                            "--->--->--->--->--->\"id\":18," +
                            "--->--->--->--->--->\"name\":\"Man\"," +
                            "--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->{" +
                            "--->--->--->--->--->--->--->\"id\":19," +
                            "--->--->--->--->--->--->--->\"name\":\"Casual wear\"," +
                            "--->--->--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->--->--->{\"id\":20,\"name\":\"Jacket\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->--->--->{\"id\":21,\"name\":\"Jeans\",\"childNodes\":[]}" +
                            "--->--->--->--->--->--->--->]" +
                            "--->--->--->--->--->--->},{" +
                            "--->--->--->--->--->--->--->\"id\":22," +
                            "--->--->--->--->--->--->--->\"name\":\"Formal wear\"," +
                            "--->--->--->--->--->--->--->\"childNodes\":[" +
                            "--->--->--->--->--->--->--->--->{\"id\":23,\"name\":\"Suit\",\"childNodes\":[]}," +
                            "--->--->--->--->--->--->--->--->{\"id\":24,\"name\":\"Shirt\",\"childNodes\":[]}" +
                            "--->--->--->--->--->--->--->]" +
                            "--->--->--->--->--->--->}" +
                            "--->--->--->--->--->]" +
                            "--->--->--->--->}" +
                            "--->--->--->]" +
                            "--->--->}" +
                            "--->]" +
                            "}]");
                }
        );
    }
}
