package org.babyfish.jimmer.example.save;

import org.babyfish.jimmer.example.save.common.AbstractMutationTest;
import org.babyfish.jimmer.example.save.common.ExecutedStatement;
import org.babyfish.jimmer.example.save.model.TreeNodeDraft;
import org.babyfish.jimmer.example.save.model.TreeNodeProps;
import org.babyfish.jimmer.sql.DissociateAction;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class RecursiveTest extends AbstractMutationTest {

    @Test
    public void testCreateTree() {

        sql()
                .getEntities()
                .saveCommand(
                        TreeNodeDraft.$.produce(root -> {
                            root.setParentNode(null);
                            root.setName("root");
                            root.addIntoChildNodes(child_1 -> {
                                child_1.setName("child-1");
                                child_1.addIntoChildNodes(child_1_1 -> {
                                    child_1_1.setName("child-1-1");
                                    child_1_1.setChildNodes(Collections.emptyList());
                                });
                                child_1.addIntoChildNodes(child_1_2 -> {
                                    child_1_2.setName("child-1-2");
                                    child_1_2.setChildNodes(Collections.emptyList());
                                });
                            });
                            root.addIntoChildNodes(child_2 -> {
                                child_2.setName("child-2");
                                child_2.addIntoChildNodes(child_2_1 -> {
                                    child_2_1.setName("child-2-1");
                                    child_2_1.setChildNodes(Collections.emptyList());
                                });
                                child_2.addIntoChildNodes(child_2_2 -> {
                                    child_2_2.setName("child-2-2");
                                    child_2_2.setChildNodes(Collections.emptyList());
                                });
                            });
                        })
                )
                .setAutoAttaching(TreeNodeProps.CHILD_NODES)
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id is null",
                        "root"
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "root", null
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-1", 1L
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "child-1", 1L
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-1-1", 2L
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "child-1-1", 2L
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-1-2", 2L
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "child-1-2", 2L
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-2", 1L
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "child-2", 1L
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-2-1", 5L
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "child-2-1", 5L
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-2-2", 5L
                ),
                new ExecutedStatement(
                        "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                        "child-2-2", 5L
                )
        );
    }

    @Test
    public void testDeleteSubTrees() {
        jdbc(
                "insert into tree_node(node_id, name, parent_id) values" +
                        "(1, 'root', null)," +
                        "    (2, 'child-1', 1)," +
                        "        (3, 'child-1-1', 2)," +
                        "        (4, 'child-1-2', 2)," +
                        "    (5, 'child-2', 1)," +
                        "        (6, 'child-2-1', 5)," +
                        "        (7, 'child-2-2', 5)"
        );

        sql()
                .getEntities()
                .saveCommand(
                        TreeNodeDraft.$.produce(root -> {
                            root.setParentNode(null);
                            root.setName("root");
                            root.addIntoChildNodes(child_1 -> {
                                child_1.setName("child-1");
                                child_1.addIntoChildNodes(child_1_1 -> {
                                    child_1_1.setName("child-1-1");
                                    child_1_1.setChildNodes(Collections.emptyList());
                                });
                                // `child-1-2` will be deleted
                            });
                            // `-+-child-2`
                            // ` |`
                            // ` +----child-2-1`
                            // ` |`
                            // `-|----child-2-2`
                            // will be deleted
                        })
                )
                .setDissociateAction(TreeNodeProps.PARENT_NODE, DissociateAction.DELETE)
                .execute();

        assertExecutedStatements(
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id is null",
                        "root"
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-1", 1L
                ),
                new ExecutedStatement(
                        "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                                "from TREE_NODE as tb_1_ " +
                                "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                        "child-1-1", 2L
                ),
                new ExecutedStatement(
                        "select node_id from TREE_NODE where parent_id = ?",
                        3L
                ),
                new ExecutedStatement(
                        "select node_id " +
                                "from TREE_NODE " +
                                "where parent_id = ? and node_id not in (?)",
                        2L, 3L
                ),
                new ExecutedStatement(
                        "select node_id from TREE_NODE where parent_id in (?)",
                        4L
                ),
                new ExecutedStatement(
                        "delete from TREE_NODE where node_id in (?)",
                        4L
                ),
                new ExecutedStatement(
                        "select node_id " +
                                "from TREE_NODE " +
                                "where parent_id = ? and node_id not in (?)",
                        1L, 2L
                ),
                new ExecutedStatement(
                        "select node_id from TREE_NODE where parent_id in (?)",
                        5L
                ),
                new ExecutedStatement(
                        "select node_id from TREE_NODE where parent_id in (?, ?)",
                        6L, 7L
                ),
                new ExecutedStatement(
                        "delete from TREE_NODE where node_id in (?, ?)",
                        6L, 7L
                ),
                new ExecutedStatement(
                        "delete from TREE_NODE where node_id in (?)",
                        5L
                )
        );
    }
}
