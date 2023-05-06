package org.babyfish.jimmer.example.save

import org.babyfish.jimmer.example.save.common.AbstractMutationTest
import org.babyfish.jimmer.example.save.common.ExecutedStatement
import org.babyfish.jimmer.example.save.model.TreeNode
import org.babyfish.jimmer.example.save.model.addBy
import org.babyfish.jimmer.example.save.model.by
import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.DissociateAction
import org.junit.jupiter.api.Test


/**
 * Recommended learning sequence: 6
 *
 *
 * SaveModeTest -> IncompleteObjectTest -> ManyToOneTest ->
 * OneToManyTest -> ManyToManyTest -> [current: RecursiveTest] -> TriggerTest
 */
class RecursiveTest : AbstractMutationTest() {

    /*
     * Noun explanation
     *
     * Short Association: Association object(s) with only id property.
     * Long Association: Association object(s) with non-id properties.
     */

    @Test
    fun testCreateTree() {
        sql
            .entities
            .save(
                /*
                 * `TreeNode` has two key properties: `name` and `parentNode`,
                 * this means `name` and `parentNode` must be specified when `id` is missing.
                 *
                 * One-to-many association is special, parent object can specify the
                 * many-to-one association of its child objects implicitly.
                 * In this demo, Associations named `childNodes` specify `parentNode`
                 * for child objects implicitly so that all child objects do not require
                 * the `parentNode`.
                 *
                 * However, the `parentNode` of ROOT cannot be specified implicitly,
                 * so that it must be specified manually
                 */
                new(TreeNode::class).by {
                    parentNode = null
                    name = "root"
                    childNodes().addBy {
                        name = "child-1"
                        childNodes().addBy {
                            name = "child-1-1"
                            childNodes = emptyList()
                        }
                        childNodes().addBy {
                            name = "child-1-2"
                            childNodes = emptyList()
                        }
                    }
                    childNodes().addBy {
                        name = "child-2"
                        childNodes().addBy {
                            name = "child-2-1"
                            childNodes = emptyList()
                        }
                        childNodes().addBy {
                            name = "child-2-2"
                            childNodes = emptyList()
                        }
                    }
                }
            ) {
                /*
                 * You can also use `setAutoAttachingAll()`.
                 *
                 * If you use jimmer-spring-starter, it is unnecessary to
                 * do it because this switch is turned on.
                 */
                setAutoAttaching(TreeNode::childNodes)
            }

        assertExecutedStatements(

            // Query root
            ExecutedStatement(
                "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id is null",
                "root"
            ),

            // Root does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "root", null
            ),

            // Query `child-1`
            ExecutedStatement(
                "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                "child-1", 1L
            ),

            // `child-1` does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "child-1", 1L
            ),

            // Query `child-1-1`
            ExecutedStatement(
                ("select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?"),
                "child-1-1", 2L
            ),

            // `child-1-1` does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "child-1-1", 2L
            ),

            // Query `child-1-2`
            ExecutedStatement(
                ("select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?"),
                "child-1-2", 2L
            ),

            // `child-1-2` does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "child-1-2", 2L
            ),

            // Query `child-2`
            ExecutedStatement(
                ("select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?"),
                "child-2", 1L
            ),

            // `child-2` does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "child-2", 1L
            ),

            // Query `child-2-1`
            ExecutedStatement(
                ("select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?"),
                "child-2-1", 5L
            ),

            // `child-2-1` does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "child-2-1", 5L
            ),

            // Query `child-2-2`
            ExecutedStatement(
                ("select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?"),
                "child-2-2", 5L
            ),

            // `child-2-2` does not exist, insert it
            ExecutedStatement(
                "insert into TREE_NODE(NAME, parent_id) values(?, ?)",
                "child-2-2", 5L
            )
        )
    }

    @Test
    fun testDeleteSubTrees() {
        jdbc(
            "insert into tree_node(node_id, name, parent_id) values" +
                "(1, 'root', null)," +
                "    (2, 'child-1', 1)," +
                "        (3, 'child-1-1', 2)," +
                "        (4, 'child-1-2', 2)," +
                "    (5, 'child-2', 1)," +
                "        (6, 'child-2-1', 5)," +
                "        (7, 'child-2-2', 5)"
        )

        sql
            .entities
            .save(
                // Please view the comment of `testCreateTree` to understand
                // why `parentNode` is a key property of `TreeNode`
                // but only the root node needs it.
                new(TreeNode::class).by {
                    parentNode = null
                    name = "root"
                    childNodes().addBy {
                        name = "child-1"
                        childNodes().addBy {
                            name = "child-1-1"
                            childNodes = emptyList()
                        }
                        // `child-1-2` in database will be deleted
                    }
                    // `-+-child-2`
                    // ` |`
                    // ` +----child-2-1`
                    // ` |`
                    // `-|----child-2-2`
                    // in database will be deleted
                }
            )

        assertExecutedStatements(

            // Query aggregate by key
            ExecutedStatement(
                "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id is null",
                "root"
            ),

            // Aggregate-root exists, but not changed, do nothing

            // Query `child-1` by key
            ExecutedStatement(
                "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                "child-1", 1L
            ),

            // `child-1` exists, but not changed, do nothing

            // Query `child-1-1` by key
            ExecutedStatement(
                "select tb_1_.node_id, tb_1_.NAME, tb_1_.parent_id " +
                    "from TREE_NODE tb_1_ " +
                    "where tb_1_.NAME = ? and tb_1_.parent_id = ?",
                "child-1-1", 2L
            ),

            // `child-1-1` exists, but not changed, do nothing

            // Query child nodes of `child-1-1`
            ExecutedStatement(
                "select node_id from TREE_NODE where parent_id = ?",
                3L
            ),

            // `child-1-1` does not have child nodes, do nothing

            // Query child nodes of `child-1` except `child-1-1`
            ExecutedStatement(
                "select node_id " +
                    "from TREE_NODE " +
                    "where parent_id = ? and node_id not in (?)",
                2L, 3L
            ),

            // Query child nodes of `child-1-2`
            ExecutedStatement(
                "select node_id from TREE_NODE where parent_id in (?)",
                4L
            ),

            // `child-1-2` does not have child nodes, do nothing

            // Delete `child-1-2`
            ExecutedStatement(
                "delete from TREE_NODE where node_id in (?)",
                4L
            ),

            // Query child nodes of root except `child-1`
            ExecutedStatement(
                ("select node_id " +
                    "from TREE_NODE " +
                    "where parent_id = ? and node_id not in (?)"),
                1L, 2L
            ),

            // Query child nodes of `child-2`
            ExecutedStatement(
                "select node_id from TREE_NODE where parent_id in (?)",
                5L
            ),

            // Query child nodes of `child-2-1` and `child-2-2`
            ExecutedStatement(
                "select node_id from TREE_NODE where parent_id in (?, ?)",
                6L, 7L
            ),

            // `child-2-1` and `child-2-2` does not have child nodes, do nothing

            // Delete `child-2-1` and `child-2-2`
            ExecutedStatement(
                "delete from TREE_NODE where node_id in (?, ?)",
                6L, 7L
            ),

            // Delete `child-2`
            ExecutedStatement(
                "delete from TREE_NODE where node_id in (?)",
                5L
            )
        )
    }
}