package org.babyfish.jimmer;

import org.babyfish.jimmer.model.ListState;
import org.babyfish.jimmer.model.ListStateDraft;
import org.babyfish.jimmer.model.TreeNode;
import org.babyfish.jimmer.model.TreeNodeDraft;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ListDraft;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ListIdentityTest {

    @Test
    public void namedAndIndexedIdsAddressTheSameListDraft() {
        ListState base = ListStateDraft.$.produce(d -> {
            d.setLeft(Collections.singletonList("base"));
            d.setRight(Collections.emptyList());
            d.setOptional(Collections.emptyList());
            d.setNodes(Collections.emptyList());
        });
        ListState changed = ListStateDraft.$.produce(base, d -> {
            DraftSpi spi = (DraftSpi) d;
            for (String name : Arrays.asList("left", "right", "optional", "nodes")) {
                PropId named = PropId.byName(name);
                PropId indexed = spi.__type().getProp(name).getId();
                assertNull(spi.__getListDraft(named));
                ListDraft<?> list = (ListDraft<?>) spi.__get(indexed);
                assertSame(list, spi.__getListDraft(named));
                spi.__setListDraft(named, null);
                assertNull(spi.__getListDraft(indexed));
                spi.__setListDraft(named, list);
                assertSame(list, spi.__getListDraft(indexed));
                spi.__setListDraft(indexed, null);
                assertNull(spi.__getListDraft(named));
                spi.__setListDraft(indexed, list);
                assertSame(list, spi.__getListDraft(named));
            }
            List<String> namedList = spi.__draftContext().toDraftList(spi, PropId.byName("left"), base.left(), String.class, false);
            assertSame(d.left(), namedList);
            namedList.add("changed");
            assertNull(spi.__getListDraft(PropId.byName("missing")));
            assertThrows(IllegalArgumentException.class, () -> spi.__setListDraft(PropId.byName("missing"), null));
            assertNull(spi.__getListDraft(PropId.byIndex(1000)));
            assertThrows(IllegalArgumentException.class, () -> spi.__setListDraft(PropId.byIndex(1000), null));
        });
        assertEquals(Arrays.asList("base", "changed"), changed.left());
        assertEquals(Collections.singletonList("base"), base.left());
        TreeNodeDraft.$.produce(d -> {
            DraftSpi spi = (DraftSpi) d;
            PropId named = PropId.byName("name");
            PropId indexed = spi.__type().getProp("name").getId();
            assertNull(spi.__getListDraft(named));
            assertNull(spi.__getListDraft(indexed));
            assertThrows(IllegalArgumentException.class, () -> spi.__setListDraft(named, null));
            assertThrows(IllegalArgumentException.class, () -> spi.__setListDraft(indexed, null));
        });
    }

    @Test
    public void sharedListsBelongToTheirProperties() {
        for (List<String> shared : Arrays.asList(Collections.<String>emptyList(), Collections.singletonList("a"))) {
            ListState base = ListStateDraft.$.produce(d -> {
                d.setLeft(shared);
                d.setRight(shared);
            });
            ListState changed = ListStateDraft.$.produce(base, d -> {
                assertSame(d.left(), d.left(true));
                d.left().add("b");
                assertEquals(shared, d.right());
            });
            assertEquals(shared, base.left());
            assertEquals(shared, changed.right());
            assertEquals(shared.size() + 1, changed.left().size());
        }
    }

    @Test
    public void sharedListsBelongToTheirOwners() {
        List<TreeNode> empty = Collections.emptyList();
        TreeNode first = TreeNodeDraft.$.produce(d -> d.setChildNodes(empty));
        TreeNode second = TreeNodeDraft.$.produce(d -> d.setChildNodes(first.childNodes()));
        ListState base = ListStateDraft.$.produce(d -> d.setNodes(Arrays.asList(first, second)));
        ListState changed = ListStateDraft.$.produce(base, d -> {
            TreeNodeDraft node = (TreeNodeDraft) d.nodes().get(0);
            node.addIntoChildNodes(c -> c.setName("child"));
            assertTrue(d.nodes().get(1).childNodes().isEmpty());
        });
        assertEquals(1, changed.nodes().get(0).childNodes().size());
        assertTrue(changed.nodes().get(1).childNodes().isEmpty());
        assertTrue(first.childNodes().isEmpty());
    }

    @Test
    public void replacingAndUnloadingDetachOldDraftLists() {
        ListState base = ListStateDraft.$.produce(d -> d.setLeft(Collections.singletonList("base")));
        ListState changed = ListStateDraft.$.produce(base, d -> {
            List<String> old = d.left();
            old.add("old");
            d.setLeft(Collections.singletonList("replacement"));
            old.add("detached");
            assertEquals(Collections.singletonList("replacement"), d.left());
            d.left().add("new");
            DraftObjects.unload(d, "left");
            d.setLeft(base.left());
            assertEquals(Collections.singletonList("base"), d.left());
            d.left().add("last");
        });
        assertEquals(Arrays.asList("base", "last"), changed.left());
        assertEquals(Collections.singletonList("base"), base.left());
    }

    @Test
    public void untouchedListsAndObjectsAreReused() {
        ListState base = ListStateDraft.$.produce(d -> d.setLeft(Collections.singletonList("base")));
        assertSame(base, ListStateDraft.$.produce(base, d -> d.left()));
        assertSame(base, ListStateDraft.$.produce(base, d -> {
            d.left().add("temporary");
            d.left().remove("temporary");
        }));
        ListState changed = ListStateDraft.$.produce(base, d -> d.setRight(Collections.emptyList()));
        assertSame(base.left(), changed.left());
    }

    @Test
    public void replacementDetachesEvenWhenOriginalListIsAssignedAgain() {
        List<String> shared = Collections.unmodifiableList(Collections.singletonList("base"));
        ListState base = ListStateDraft.$.produce(d -> {
            d.setLeft(shared);
            d.setRight(shared);
        });
        assertSame(shared, base.left());
        assertSame(base.left(), base.right());
        ListState changed = ListStateDraft.$.produce(base, d -> {
            List<String> old = d.left();
            old.add("detached");
            d.setLeft(Collections.emptyList());
            d.setLeft(shared);
        });
        assertEquals(Collections.singletonList("base"), changed.left());
    }

    @Test
    public void assigningDraftListsKeepsPropertyMutationsIndependent() {
        ListState changed = ListStateDraft.$.produce(d -> {
            d.setLeft(Collections.singletonList("base"));
            d.setRight(d.left());
            d.right().add("right");
        });
        assertEquals(Collections.singletonList("base"), changed.left());
        assertEquals(Arrays.asList("base", "right"), changed.right());
        assertThrows(UnsupportedOperationException.class, changed.left()::clear);
        assertThrows(UnsupportedOperationException.class, changed.right()::clear);
    }

    @Test
    public void nullableListsPreserveLoadedNullAndAllowCreation() {
        ListState base = ListStateDraft.$.produce(d -> d.setOptional(null));
        assertTrue(ImmutableObjects.isLoaded(base, "optional"));
        assertNull(base.optional());
        assertSame(base, ListStateDraft.$.produce(base, d -> assertNull(d.optional())));
        ListState changed = ListStateDraft.$.produce(base, d -> d.optional(true).add("created"));
        assertEquals(Collections.singletonList("created"), changed.optional());
        assertNull(base.optional());
    }

    @Test
    public void entityElementsRemainSharedBetweenLists() {
        TreeNode node = TreeNodeDraft.$.produce(d -> d.setName("base"));
        ListState base = ListStateDraft.$.produce(d -> d.setNodes(Arrays.asList(node, node)));
        ListState changed = ListStateDraft.$.produce(base, d -> {
            assertSame(d.nodes().get(0), d.nodes().get(1));
            ((TreeNodeDraft) d.nodes().get(0)).setName("changed");
        });
        assertSame(changed.nodes().get(0), changed.nodes().get(1));
        assertEquals("changed", changed.nodes().get(1).name());
        assertEquals("base", node.name());
    }

    @Test
    public void readOnlyListsPreserveRandomAccessAndRejectMutations() {
        for (List<String> input : Arrays.asList(new ArrayList<>(Arrays.asList("a", "b")), new LinkedList<>(Arrays.asList("a", "b")))) {
            List<String> list = ListStateDraft.$.produce(d -> d.setLeft(input)).left();
            assertEquals(input instanceof RandomAccess, list instanceof RandomAccess);
            assertThrows(UnsupportedOperationException.class, () -> list.add("c"));
            assertThrows(UnsupportedOperationException.class, () -> list.set(0, "c"));
            assertThrows(UnsupportedOperationException.class, () -> list.subList(0, 1).clear());
            ListIterator<String> iterator = list.listIterator();
            iterator.next();
            assertThrows(UnsupportedOperationException.class, iterator::remove);
            assertThrows(UnsupportedOperationException.class, () -> iterator.set("c"));
        }
    }
}
