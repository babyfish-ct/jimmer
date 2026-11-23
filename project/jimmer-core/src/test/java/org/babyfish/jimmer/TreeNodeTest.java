package org.babyfish.jimmer;

import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.model.TreeNode;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import static org.babyfish.jimmer.support.JacksonBackend.Naming.*;

public class TreeNodeTest {

    @JacksonTest
    public void test(JacksonBackend backend) {
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
                backend.write(treeNode)
        );
        Assertions.assertEquals(
                newTreeNodeText.replace("--->", ""),
                backend.write(newTreeNode)
        );
    }

    @JacksonTest
    public void testJsonProperty(JacksonBackend backend) throws Exception {
        TreeNode treeNode = TreeNodeDraft.$.produce(child -> {
            child.setName("child");
            child.applyParent(parent -> {
                parent.setName("parent");
            });
        });
        String json = backend.write(treeNode);
        Assertions.assertEquals(
                "{\"name\":\"child\",\"TheParentNode\":{\"name\":\"parent\"}}",
                json
        );
        Assertions.assertEquals(
                treeNode,
                backend.codec.readerFor(TreeNode.class).read(json)
        );
    }

    @JacksonTest
    public void testJsonAlias(JacksonBackend backend) throws Exception {
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
                backend.write(backend.codec.readerFor(TreeNode.class).read(
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
                ))
        );
    }

    @JacksonTest
    public void testNamingStrategy(JacksonBackend backend) throws Exception {
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
        JsonCodec codec = backend.naming(SNAKE_CASE);

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
                backend.write(codec.readerFor(TreeNode.class).read(json))
                        .replace("childNodes", "child_nodes")
        );
    }
}
