package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
import org.babyfish.jimmer.sql.model.mydto.RecursiveTree2;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RecursiveTree2Test extends AbstractQueryTest {

    @Test
    public void testDtoToEntity() {
        RecursiveTree2 tree = root(
                "food",
                tree(
                        "drinks",
                        deepTree("cocacola"),
                        deepTree("fenta")
                )
        );
        assertContentEquals(
                "{" +
                        "--->\"name\":\"food\"," +
                        "--->\"childNodes\":[" +
                        "--->--->{" +
                        "--->--->--->\"name\":\"drinks\"," +
                        "--->--->--->\"childNodes\":[" +
                        "--->--->--->--->{" +
                        "--->--->--->--->--->\"name\":\"cocacola\"," +
                        "--->--->--->--->--->\"childNodes\":[]" +
                        "--->--->--->--->},{" +
                        "--->--->--->--->--->\"name\":\"fenta\"," +
                        "--->--->--->--->--->\"childNodes\":[]" +
                        "--->--->--->--->}" +
                        "--->--->--->]" +
                        "--->--->}" +
                        "--->]" +
                        "}",
                tree.toEntity().toString()
        );
    }

    @Test
    public void testEntityToDto() {
        TreeNode treeNode = TreeNodeDraft.$.produce(food -> {
           food.setName("food").addIntoChildNodes(drinks -> {
               drinks.setName("drinks");
               drinks.addIntoChildNodes(cocacola -> {
                   cocacola.setName("Cococla");
               });
               drinks.addIntoChildNodes(fenta -> {
                   fenta.setName("Fenta");
               });
           });
        });
        assertContentEquals(
                "RecursiveTree2(" +
                        "--->name=food, " +
                        "--->childNodes=[" +
                        "--->--->RecursiveTree2.TargetOf_childNodes(" +
                        "--->--->--->name=drinks, " +
                        "--->--->--->childNodes=[" +
                        "--->--->--->--->RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2(" +
                        "--->--->--->--->--->name=Cococla, " +
                        "--->--->--->--->--->childNodes=null" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2(" +
                        "--->--->--->--->--->name=Fenta, " +
                        "--->--->--->--->--->childNodes=null" +
                        "--->--->--->--->)" +
                        "--->--->--->]" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                new RecursiveTree2(treeNode)
        );
    }

    @Test
    public void testEntityToDto2() {
        TreeNode treeNode = TreeNodeDraft.$.produce(food -> {
            food.setName("food").addIntoChildNodes(drinks -> {
                drinks.setName("drinks");
                drinks.addIntoChildNodes(cocacola -> {
                    cocacola.setName("Cococla");
                    cocacola.setChildNodes(Collections.emptyList());
                });
                drinks.addIntoChildNodes(fenta -> {
                    fenta.setName("Fenta");
                    fenta.setChildNodes(Collections.emptyList());
                });
            });
        });
        assertContentEquals(
                "RecursiveTree2(" +
                        "--->name=food, " +
                        "--->childNodes=[" +
                        "--->--->RecursiveTree2.TargetOf_childNodes(" +
                        "--->--->--->name=drinks, " +
                        "--->--->--->childNodes=[" +
                        "--->--->--->--->RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2(" +
                        "--->--->--->--->--->name=Cococla, " +
                        "--->--->--->--->--->childNodes=[]" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2(" +
                        "--->--->--->--->--->name=Fenta, " +
                        "--->--->--->--->--->childNodes=[]" +
                        "--->--->--->--->)" +
                        "--->--->--->]" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                new RecursiveTree2(treeNode)
        );
    }

    @Test
    public void testFindTree() {

        TreeNodeTable table = TreeNodeTable.$;

        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.parentId().isNull())
                        .select(table.fetch(RecursiveTree2.class)),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID is null"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID = ? " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(2).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(3).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(4).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    ctx.statement(5).sql(
                            "select tb_1_.PARENT_ID, tb_1_.NODE_ID, tb_1_.NAME " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID in (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                    "order by tb_1_.NODE_ID asc"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"name\":\"Home\"," +
                                    "--->--->\"childNodes\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"Food\"," +
                                    "--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"name\":\"Drinks\"," +
                                    "--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Coca Cola\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Fanta\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->}," +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"name\":\"Bread\"," +
                                    "--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Baguette\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Ciabatta\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"name\":\"Clothing\"," +
                                    "--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"name\":\"Woman\"," +
                                    "--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Casual wear\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Dress\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Miniskirt\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Jeans\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Formal wear\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Suit\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Shirt\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->}," +
                                    "--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->\"name\":\"Man\"," +
                                    "--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Casual wear\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Jacket\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Jeans\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->\"name\":\"Formal wear\"," +
                                    "--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Suit\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}," +
                                    "--->--->--->--->--->--->--->--->--->{" +
                                    "--->--->--->--->--->--->--->--->--->--->\"name\":\"Shirt\"," +
                                    "--->--->--->--->--->--->--->--->--->--->\"childNodes\":[" +
                                    "--->--->--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->--->--->}" +
                                    "--->--->--->--->--->--->]" +
                                    "--->--->--->--->--->}" +
                                    "--->--->--->--->]" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    void nonNullIsNull() {
        TreeNodeTable table = TreeNodeTable.$;

        assertThrows(
                IllegalArgumentException.class,
                () ->  getSqlClient()
                        .createQuery(table)
                        .where(table.parent().id().isNull())
                        .select(table.fetch(RecursiveTree2.class))
        );
    }

    private static RecursiveTree2 root(String name, RecursiveTree2.TargetOf_childNodes ...childNodes) {
        RecursiveTree2 tree = new RecursiveTree2();
        tree.setName(name);
        if (childNodes.length > 0) {
            tree.setChildNodes(Arrays.asList(childNodes));
        }
        return tree;
    }

    private static RecursiveTree2.TargetOf_childNodes tree(String name, RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2 ...childNodes) {
        RecursiveTree2.TargetOf_childNodes tree = new RecursiveTree2.TargetOf_childNodes();
        tree.setName(name);
        if (childNodes.length > 0) {
            tree.setChildNodes(Arrays.asList(childNodes));
        }
        return tree;
    }

    private static RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2 deepTree(String name, RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2 ...childNodes) {
        RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2 tree = new RecursiveTree2.TargetOf_childNodes.TargetOf_childNodes_2();
        tree.setName(name);
        if (childNodes.length > 0) {
            tree.setChildNodes(Arrays.asList(childNodes));
        }
        return tree;
    }
}
