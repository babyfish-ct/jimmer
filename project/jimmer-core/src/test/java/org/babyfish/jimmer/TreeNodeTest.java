package org.babyfish.jimmer;

import org.babyfish.jimmer.jackson.codec.JsonCodec;
import org.babyfish.jimmer.model.TreeNode;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.babyfish.jimmer.jackson.codec.PropertyNamingCustomization;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.jackson.codec.JsonCodec.jsonCodec;
import static org.babyfish.jimmer.jackson.codec.PropertyNamingCustomization.PropertyNaming.SNAKE_CASE;

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
    public void testJsonProperty() throws Exception {
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
                jsonCodec().readerFor(TreeNode.class).read(json)
        );
    }

    @Test
    public void testJsonAlias() throws Exception {
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
                jsonCodec().readerFor(TreeNode.class).read(
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
                        ).replace("--->", "")
                ).toString()
        );
    }

    @Test
    public void testNamingStrategy() throws Exception {
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
        JsonCodec<?> codec = jsonCodec().withCustomizations(new PropertyNamingCustomization(SNAKE_CASE));

        String json =
                "{" +
                        "--->\"name\":\"Root\"," +
                        "--->\"child_nodes\":[" +
                        "--->--->{" +
                        "--->--->--->\"name\":\"Food\"," +
                        "--->--->--->\"child_nodes\":[" +
                        "--->--->--->--->{" +
                        "--->--->--->--->--->\"name\":\"Drink\"," +
                        "--->--->--->--->--->\"child_nodes\":[" +
                        "--->--->--->--->--->--->{\"name\":\"Coco Cola\"}," +
                        "--->--->--->--->--->--->{\"name\":\"Fanta\"}" +
                        "--->--->--->--->--->]" +
                        "--->--->--->--->}" +
                        "--->--->--->]" +
                        "--->--->}" +
                        "--->]" +
                        "}";
        json = json.replace("--->", "");
        Assertions.assertEquals(
                json,
                codec.writer().writeAsString(treeNode)
        );
        Assertions.assertEquals(
                json,
                codec.readerFor(TreeNode.class).read(json).toString()
                        .replace("childNodes", "child_nodes")
        );
    }
}
