package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.model.dto.RecursiveTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class RecursiveTreeTest extends Tests {

    @Test
    public void testDtoToEntity() {
        RecursiveTree tree = root(
                "food",
                tree(
                        "drinks",
                        tree("cocacola"),
                        tree("fenta")
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
                "RecursiveTree(" +
                        "--->name=food, " +
                        "--->childNodes=[" +
                        "--->--->RecursiveTree.TargetOf_childNodes(" +
                        "--->--->--->name=drinks, " +
                        "--->--->--->childNodes=[" +
                        "--->--->--->--->RecursiveTree.TargetOf_childNodes(" +
                        "--->--->--->--->--->name=Cococla, " +
                        "--->--->--->--->--->childNodes=null" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree.TargetOf_childNodes(" +
                        "--->--->--->--->--->name=Fenta, " +
                        "--->--->--->--->--->childNodes=null" +
                        "--->--->--->--->)" +
                        "--->--->--->]" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                RecursiveTree.of(treeNode)
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
                "RecursiveTree(" +
                        "--->name=food, " +
                        "--->childNodes=[" +
                        "--->--->RecursiveTree.TargetOf_childNodes(" +
                        "--->--->--->name=drinks, " +
                        "--->--->--->childNodes=[" +
                        "--->--->--->--->RecursiveTree.TargetOf_childNodes(" +
                        "--->--->--->--->--->name=Cococla, " +
                        "--->--->--->--->--->childNodes=null" +
                        "--->--->--->--->), " +
                        "--->--->--->--->RecursiveTree.TargetOf_childNodes(" +
                        "--->--->--->--->--->name=Fenta, " +
                        "--->--->--->--->--->childNodes=null" +
                        "--->--->--->--->)" +
                        "--->--->--->]" +
                        "--->--->)" +
                        "--->]" +
                        ")",
                RecursiveTree.of(treeNode)
        );
    }

    private static RecursiveTree root(String name, RecursiveTree.TargetOf_childNodes ...childNodes) {
        RecursiveTree tree = new RecursiveTree();
        tree.setName(name);
        if (childNodes.length > 0) {
            tree.setChildNodes(Arrays.asList(childNodes));
        }
        return tree;
    }

    private static RecursiveTree.TargetOf_childNodes tree(String name, RecursiveTree.TargetOf_childNodes ...childNodes) {
        RecursiveTree.TargetOf_childNodes tree = new RecursiveTree.TargetOf_childNodes();
        tree.setName(name);
        if (childNodes.length > 0) {
            tree.setChildNodes(Arrays.asList(childNodes));
        }
        return tree;
    }
}
