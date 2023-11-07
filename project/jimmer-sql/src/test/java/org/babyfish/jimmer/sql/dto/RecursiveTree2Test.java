package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.common.Tests;
import org.babyfish.jimmer.sql.model.TreeNode;
import org.babyfish.jimmer.sql.model.TreeNodeDraft;
import org.babyfish.jimmer.sql.model.dto.RecursiveTree2;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

public class RecursiveTree2Test extends Tests {

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
                RecursiveTree2.of(treeNode)
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
                RecursiveTree2.of(treeNode)
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
