package org.babyfish.jimmer;

import org.babyfish.jimmer.model.TreeNode;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TreeNodeTest {

    @Test
    public void test() {
        TreeNode treeNode = TreeNodeDraft.$.produce(root -> {
            root.setName("Root").addIntoChildNodes(food -> {
                food
                        .setName("Food")
                        .addIntoChildNodes(drink -> {
                            drink
                                    .setName("Drink")
                                    .addIntoChildNodes(cococola -> {
                                        cococola.setName("Coco Cola");
                                    })
                                    .addIntoChildNodes(fanta -> {
                                        fanta.setName("Fanta");
                                    });
                            ;
                        });
                ;
            });
        });

        TreeNode newTreeNode = TreeNodeDraft.$.produce(
                // highlight-next-line
                treeNode, // 现有的数据对象
                root -> {
                    root
                            .childNodes(true).get(0)
                            .childNodes(true).get(0)
                            .childNodes(true).get(0)
                            .setName("Coco Cola plus");
                }
        );

        String treeNodeText = "{" +
                "--->\"name\":\"Root\"," +
                "--->\"childNodes\":[" +
                "--->--->{" +
                "--->--->--->\"name\":\"Food\"," +
                "--->--->--->\"childNodes\":[" +
                "--->--->--->--->{" +
                "--->--->--->--->--->\"name\":\"Drink\"," +
                "--->--->--->--->--->\"childNodes\":[" +
                "--->--->--->--->--->--->{\"name\":\"Coco Cola\"}," +
                "--->--->--->--->--->--->{\"name\":\"Fanta\"}" +
                "--->--->--->--->--->]" +
                "--->--->--->--->}" +
                "--->--->--->]" +
                "--->--->}" +
                "--->]" +
                "}";

        String newTreeNodeText = "{" +
                "--->\"name\":\"Root\"," +
                "--->\"childNodes\":[" +
                "--->--->{" +
                "--->--->--->\"name\":\"Food\"," +
                "--->--->--->\"childNodes\":[" +
                "--->--->--->--->{" +
                "--->--->--->--->--->\"name\":\"Drink\"," +
                "--->--->--->--->--->\"childNodes\":[" +
                "--->--->--->--->--->--->{\"name\":\"Coco Cola plus\"}," +
                "--->--->--->--->--->--->{\"name\":\"Fanta\"}" +
                "--->--->--->--->--->]" +
                "--->--->--->--->}" +
                "--->--->--->]" +
                "--->--->}" +
                "--->]" +
                "}";

        Assertions.assertEquals(
                treeNodeText.replace("--->", ""),
                treeNode.toString()
        );
        Assertions.assertEquals(
                newTreeNodeText.replace("--->", ""),
                newTreeNode.toString()
        );
    }
}
