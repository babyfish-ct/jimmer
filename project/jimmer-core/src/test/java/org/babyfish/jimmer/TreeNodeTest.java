package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.ImmutableModule;
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

    @Test
    public void testJsonProperty() throws JsonProcessingException {
        TreeNode treeNode = TreeNodeDraft.$.produce(child -> {
            child.setName("child");
            child.applyParent(parent -> {
                parent.setName("parent");
            });
        });
        String json = treeNode.toString();
        Assertions.assertEquals(
                "{\"name\":\"child\",\"TheParentNode\":{\"name\":\"parent\"}}",
                json
        );
        Assertions.assertEquals(
                treeNode,
                new ObjectMapper().registerModule(new ImmutableModule()).readValue(json, TreeNode.class)
        );
    }

    @Test
    public void testJsonAlias() throws JsonProcessingException {
        Assertions.assertEquals(
                (
                        "{" +
                        "--->\"name\":\"Root\"," +
                        "--->\"childNodes\":[" +
                        "--->--->{" +
                        "--->--->--->\"name\":\"Food\"," +
                        "--->--->--->\"childNodes\":[" +
                        "--->--->--->--->{" +
                        "--->--->--->--->--->\"name\":\"Drink\"," +
                        "--->--->--->--->--->\"childNodes\":[" +
                        "--->--->--->--->--->--->{" +
                        "--->--->--->--->--->--->--->\"name\":\"Coco Cola\"" +
                        "--->--->--->--->--->--->}," +
                        "--->--->--->--->--->--->{" +
                        "--->--->--->--->--->--->--->\"name\":\"Fanta\"" +
                        "--->--->--->--->--->--->}" +
                        "--->--->--->--->--->]" +
                        "--->--->--->--->}" +
                        "--->--->--->]" +
                        "--->--->}" +
                        "--->]" +
                        "}"
                ).replace("--->", ""),
                new ObjectMapper().registerModule(new ImmutableModule()).readValue(
                        (
                                "{" +
                                "--->\"name\":\"Root\"," +
                                "--->\"children\":[" +
                                "--->--->{" +
                                "--->--->--->\"name\":\"Food\"," +
                                "--->--->--->\"all-child-nodes\":[" +
                                "--->--->--->--->{" +
                                "--->--->--->--->--->\"name\":\"Drink\"," +
                                "--->--->--->--->--->\"all-child-nodes\":[" +
                                "--->--->--->--->--->--->{\"name\":\"Coco Cola\"}," +
                                "--->--->--->--->--->--->{\"name\":\"Fanta\"}" +
                                "--->--->--->--->--->]" +
                                "--->--->--->--->}" +
                                "--->--->--->]" +
                                "--->--->}" +
                                "--->]" +
                                "}"
                        ).replace("--->", ""),
                        TreeNode.class
                ).toString()
        );
    }
}
