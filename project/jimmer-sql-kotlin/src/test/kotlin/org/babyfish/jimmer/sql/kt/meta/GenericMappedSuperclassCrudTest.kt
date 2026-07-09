package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.sql.ast.mutation.DeleteMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.kt.model.generic.KGenericTreeNode
import org.babyfish.jimmer.sql.kt.model.generic.by
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenericMappedSuperclassCrudTest : AbstractMutationTest() {

    @Test
    fun testCrud() {
        jdbc(rollback = true) { con ->
            val root = KGenericTreeNode {
                id = 100L
                name = "root"
            }
            val insertRootCount = sqlClient.entities.forConnection(con).save(root) {
                setMode(SaveMode.INSERT_ONLY)
            }.totalAffectedRowCount
            assertEquals(1, insertRootCount)

            val child = KGenericTreeNode {
                id = 101L
                name = "child"
                parent = root
            }
            val insertChildCount = sqlClient.entities.forConnection(con).save(child) {
                setMode(SaveMode.INSERT_ONLY)
            }.totalAffectedRowCount
            assertEquals(2, insertChildCount)

            val fetchedRoot = sqlClient.entities.forConnection(con).findOneById(
                newFetcher(KGenericTreeNode::class).by {
                    name()
                    children {
                        name()
                    }
                },
                100L
            )
            assertEquals("root", fetchedRoot.name)
            assertEquals(1, fetchedRoot.children.size)
            assertEquals("child", fetchedRoot.children[0].name)

            val updatedChild = KGenericTreeNode {
                id = 101L
                name = "child-2"
                parent = null
            }
            val updateChildCount = sqlClient.entities.forConnection(con).save(updatedChild) {
                setMode(SaveMode.UPDATE_ONLY)
            }.totalAffectedRowCount
            assertEquals(1, updateChildCount)

            val fetchedChild = sqlClient.entities.forConnection(con).findOneById(
                newFetcher(KGenericTreeNode::class).by {
                    name()
                    parent()
                },
                101L
            )
            assertEquals("child-2", fetchedChild.name)
            assertNull(fetchedChild.parent)

            val deleteResult = sqlClient.entities.forConnection(con).delete(
                KGenericTreeNode::class,
                101L,
                null
            ) {
                setMode(DeleteMode.PHYSICAL)
            }
            assertEquals(1, deleteResult.totalAffectedRowCount)

            val deletedChild = sqlClient.entities.forConnection(con).findById(
                KGenericTreeNode::class,
                101L
            )
            assertNull(deletedChild)
        }
    }
}
