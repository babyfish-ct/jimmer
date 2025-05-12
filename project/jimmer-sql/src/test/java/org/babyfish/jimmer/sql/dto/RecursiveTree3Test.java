package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.babyfish.jimmer.sql.model.mydto.RecursiveTree3;
import org.babyfish.jimmer.sql.model.mydto.TreeNodeViewForIssue1036;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RecursiveTree3Test extends AbstractQueryTest {

    @Test
    public void test() {
        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(10L))
                        .select(table.fetch(RecursiveTree3.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.NODE_ID = ?"
                    ).variables(10L);
                    ctx.statement(1).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.NODE_ID = ?"
                    ).variables(9L);
                    ctx.statement(2).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.NODE_ID = ?"
                    ).variables(1L);
                    ctx.statement(3).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID = ? " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(10L);
                    ctx.statement(4).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(11L, 15L);
                    ctx.statement(5).sql(
                            "select " +
                                    "tb_1_.PARENT_ID, " +
                                    "tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    ).variables(12L, 13L, 14L, 16L, 17L);
                    ctx.row(0, treeNode -> {
                        assertContentEquals(
                                "RecursiveTree3(" +
                                        "--->id=10, " +
                                        "--->name=Woman, " +
                                        "--->parent=RecursiveTree3.TargetOf_parent(" +
                                        "--->--->id=9, " +
                                        "--->--->name=Clothing, " +
                                        "--->--->parent=RecursiveTree3.TargetOf_parent(" +
                                        "--->--->--->id=1, " +
                                        "--->--->--->name=Home, " +
                                        "--->--->--->parent=null" +
                                        "--->--->)" +
                                        "--->), " +
                                        "--->childNodes=[" +
                                        "--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->id=11, " +
                                        "--->--->--->name=Casual wear, " +
                                        "--->--->--->childNodes=[" +
                                        "--->--->--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->--->--->id=12, name=Dress, childNodes=[]" +
                                        "--->--->--->--->), " +
                                        "--->--->--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->--->--->id=13, name=Miniskirt, childNodes=[]" +
                                        "--->--->--->--->), " +
                                        "--->--->--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->--->--->id=14, name=Jeans, childNodes=[])" +
                                        "--->--->--->]" +
                                        "--->--->), " +
                                        "--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->id=15, " +
                                        "--->--->--->name=Formal wear, " +
                                        "--->--->--->childNodes=[" +
                                        "--->--->--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->--->--->id=16, name=Suit, childNodes=[]" +
                                        "--->--->--->--->), " +
                                        "--->--->--->--->RecursiveTree3.TargetOf_childNodes(" +
                                        "--->--->--->--->--->id=17, name=Shirt, childNodes=[]" +
                                        "--->--->--->--->)" +
                                        "--->--->--->]" +
                                        "--->--->)" +
                                        "--->]" +
                                        ")",
                                treeNode.toString()
                        );
                    });
                }
        );
    }

    @Test
    public void testForIssueIssue1036() {
        TreeNodeTable table = TreeNodeTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.parentId().isNull())
                        .select(table.fetch(TreeNodeViewForIssue1036.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE tb_1_ where tb_1_.PARENT_ID is null"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE tb_1_ where tb_1_.PARENT_ID = ? order by tb_1_.NAME asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE tb_1_ where tb_1_.PARENT_ID = ? order by tb_1_.NAME asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE tb_1_ where tb_1_.PARENT_ID in (?, ?) order by tb_1_.NAME asc"
                    );
                    ctx.statement(4).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME from TREE_NODE tb_1_ where tb_1_.PARENT_ID in (?, ?, ?, ?) order by tb_1_.NAME asc"
                    );
                    ctx.row(
                            0,
                            "{" +
                                    "--->\"id\":1," +
                                    "--->\"name\":\"Home\"," +
                                    "--->\"childNodes\":[" +
                                    "--->--->{" +
                                    "--->--->--->\"id\":9," +
                                    "--->--->--->\"name\":\"Clothing\"," +
                                    "--->--->--->\"childNodes\":null" + // Give up recursion
                                    "--->--->},{" +
                                    "--->--->--->\"id\":2," +
                                    "--->--->--->\"name\":\"Food\"," +
                                    "--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->{" +
                                    "--->--->--->--->--->\"id\":6," +
                                    "--->--->--->--->--->\"name\":\"Bread\"," +
                                    "--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->\"id\":7," +
                                    "--->--->--->--->--->--->--->\"name\":\"Baguette\"," +
                                    "--->--->--->--->--->--->--->\"childNodes\":[]" +
                                    "--->--->--->--->--->--->},{" +
                                    "--->--->--->--->--->--->--->\"id\":8," +
                                    "--->--->--->--->--->--->--->\"name\":\"Ciabatta\"," +
                                    "--->--->--->--->--->--->--->\"childNodes\":[]" +
                                    "--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->]" +
                                    "--->--->--->--->},{" +
                                    "--->--->--->--->--->\"id\":3," +
                                    "--->--->--->--->--->\"name\":\"Drinks\"," +
                                    "--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->\"id\":4," +
                                    "--->--->--->--->--->--->--->\"name\":\"Coca Cola\"," +
                                    "--->--->--->--->--->--->--->\"childNodes\":[]" +
                                    "--->--->--->--->--->--->},{" +
                                    "--->--->--->--->--->--->--->\"id\":5," +
                                    "--->--->--->--->--->--->--->\"name\":\"Fanta\"," +
                                    "--->--->--->--->--->--->--->\"childNodes\":[]" +
                                    "--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->]" +
                                    "--->--->--->--->}" +
                                    "--->--->--->]" +
                                    "--->--->}" +
                                    "--->]" +
                                    "}"
                    );
                }
        );
    }
}
