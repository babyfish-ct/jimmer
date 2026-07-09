package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.model.generic.GenericTreeNode;
import org.babyfish.jimmer.sql.model.generic.GenericTreeNodeDraft;
import org.babyfish.jimmer.sql.model.generic.GenericTreeNodeFetcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GenericMappedSuperclassCrudTest extends AbstractMutationTest {

    @Test
    public void testCrud() {
        jdbc(null, true, con -> {
            GenericTreeNode root = GenericTreeNodeDraft.$.produce(draft -> {
                draft.setId(100L);
                draft.setName("root");
            });
            int insertRootCount = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .saveCommand(root)
                    .setMode(SaveMode.INSERT_ONLY)
                    .execute()
                    .getTotalAffectedRowCount();
            Assertions.assertEquals(1, insertRootCount);

            GenericTreeNode child = GenericTreeNodeDraft.$.produce(draft -> {
                draft.setId(101L);
                draft.setName("child");
                draft.setParent(root);
            });
            int insertChildCount = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .saveCommand(child)
                    .setMode(SaveMode.INSERT_ONLY)
                    .execute()
                    .getTotalAffectedRowCount();
            Assertions.assertEquals(2, insertChildCount);

            GenericTreeNode fetchedRoot = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findOneById(
                            GenericTreeNodeFetcher.$
                                    .name()
                                    .children(GenericTreeNodeFetcher.$.name()),
                            100L
                    );
            Assertions.assertEquals("root", fetchedRoot.getName());
            Assertions.assertEquals(1, fetchedRoot.getChildren().size());
            Assertions.assertEquals("child", fetchedRoot.getChildren().get(0).getName());

            GenericTreeNode updatedChild = GenericTreeNodeDraft.$.produce(draft -> {
                draft.setId(101L);
                draft.setName("child-2");
                draft.setParent(null);
            });
            int updateChildCount = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .saveCommand(updatedChild)
                    .setMode(SaveMode.UPDATE_ONLY)
                    .execute()
                    .getTotalAffectedRowCount();
            Assertions.assertEquals(1, updateChildCount);

            GenericTreeNode fetchedChild = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findOneById(
                            GenericTreeNodeFetcher.$.name().parent(),
                            101L
                    );
            Assertions.assertEquals("child-2", fetchedChild.getName());
            Assertions.assertNull(fetchedChild.getParent());

            int deleteCount = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .delete(GenericTreeNode.class, 101L, DeleteMode.PHYSICAL)
                    .getTotalAffectedRowCount();
            Assertions.assertEquals(1, deleteCount);

            GenericTreeNode deletedChild = getSqlClient()
                    .getEntities()
                    .forConnection(con)
                    .findById(GenericTreeNode.class, 101L);
            Assertions.assertNull(deletedChild);
        });
    }
}
