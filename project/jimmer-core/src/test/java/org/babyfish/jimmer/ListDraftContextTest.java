package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.ListState;
import org.babyfish.jimmer.model.ListStateDraft;
import org.babyfish.jimmer.model.TreeNode;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.runtime.ListDraft;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ListDraftContextTest {

    @Test
    public void foreignAssociationListsAreRejectedWithAndWithoutGetter() {
        for (boolean read : Arrays.asList(false, true)) {
            for (boolean empty : Arrays.asList(false, true)) {
                DraftContext sourceContext = new DraftContext(null);
                DraftContext targetContext = new DraftContext(null);
                ListStateDraft source = draft(sourceContext);
                source.setNodes(nodes(empty));
                ListStateDraft target = draft(targetContext);
                target.setNodes(source.nodes());
                if (read) {
                    assertEquals(empty, target.nodes().isEmpty());
                }
                IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> targetContext.resolveObject(target));
                assertTrue(ex.getMessage().contains("another draft context"));
            }
        }
    }

    @Test
    public void parentAssociationListsAreRejectedWithAndWithoutGetter() {
        for (boolean read : Arrays.asList(false, true)) {
            DraftContext parent = new DraftContext(null);
            DraftContext child = new DraftContext(parent);
            ListStateDraft source = draft(parent);
            source.setNodes(Collections.emptyList());
            ListStateDraft target = draft(child);
            target.setNodes(source.nodes());
            if (read) {
                assertTrue(target.nodes().isEmpty());
            }
            assertThrows(CircularReferenceException.class, () -> child.resolveObject(target));
        }
    }

    @Test
    public void nestedListDraftsCannotHideForeignContext() {
        DraftContext sourceContext = new DraftContext(null);
        DraftContext targetContext = new DraftContext(null);
        ListStateDraft source = draft(sourceContext);
        source.setNodes(Collections.emptyList());
        List<TreeNode> nested = new ListDraft<>(targetContext, TreeNode.class, source.nodes());
        nested = new ListDraft<>(targetContext, TreeNode.class, nested);
        ListStateDraft target = draft(targetContext);
        target.setNodes(nested);
        assertTrue(target.nodes().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> targetContext.resolveObject(target));
    }

    @Test
    public void sharedContextResolvesElementsWithoutSharingListMutations() {
        DraftContext context = new DraftContext(null);
        ListStateDraft source = draft(context);
        source.setNodes(nodes(false));
        ListStateDraft target = draft(context);
        target.setNodes(source.nodes());
        ((TreeNodeDraft) target.nodes().get(0)).setName("changed");
        target.nodes().add(TreeNodeDraft.$.produce(n -> n.setName("added")));
        ListState resolvedTarget = context.resolveObject(target);
        ListState resolvedSource = context.resolveObject(source);
        assertEquals(1, resolvedSource.nodes().size());
        assertEquals(2, resolvedTarget.nodes().size());
        assertSame(resolvedSource.nodes().get(0), resolvedTarget.nodes().get(0));
        assertEquals("changed", resolvedTarget.nodes().get(0).name());
        assertFalse(resolvedTarget.nodes().get(0) instanceof Draft);
        assertThrows(UnsupportedOperationException.class, resolvedTarget.nodes()::clear);
    }

    @Test
    public void scalarDraftListsRemainContextIndependent() {
        ListStateDraft source = draft(new DraftContext(null));
        source.setLeft(Collections.singletonList("base"));
        DraftContext targetContext = new DraftContext(null);
        ListStateDraft target = draft(targetContext);
        target.setLeft(source.left());
        target.left().add("target");
        ListState resolved = targetContext.resolveObject(target);
        assertEquals(Arrays.asList("base", "target"), resolved.left());
        assertEquals(Collections.singletonList("base"), source.left());
        assertThrows(UnsupportedOperationException.class, resolved.left()::clear);
    }

    @Test
    public void builderResolvesAssignedDraftList() {
        ListStateDraft source = draft(new DraftContext(null));
        source.setNodes(nodes(false));
        ((TreeNodeDraft) source.nodes().get(0)).setName("changed");
        ListState built = new ListStateDraft.Builder().nodes(source.nodes()).build();
        assertEquals("changed", built.nodes().get(0).name());
        assertFalse(built.nodes().get(0) instanceof Draft);
        assertThrows(UnsupportedOperationException.class, built.nodes()::clear);
    }

    private static ListStateDraft draft(DraftContext context) {
        return (ListStateDraft) Internal.createDraft(context, ImmutableType.get(ListState.class), null);
    }

    private static List<TreeNode> nodes(boolean empty) {
        return empty ? Collections.emptyList() : Collections.singletonList(TreeNodeDraft.$.produce(n -> n.setName("base")));
    }
}
