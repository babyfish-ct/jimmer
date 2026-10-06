package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.babyfish.jimmer.model.BookProps;
import org.babyfish.jimmer.model.Immutables;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class BookTest {

    @Test
    public void testVisibilityOnlyChangesDoNotMutateBase() {
        for (Object prop : new Object[] {"name", PropId.byName("name"), BookProps.NAME.unwrap().getId()}) {
            Book base = BookDraft.$.produce(draft -> {
                draft.setName("base");
                DraftObjects.hide(draft, BookProps.NAME);
            });
            Book shown = BookDraft.$.produce(base, draft -> {
                if (prop instanceof String) {
                    ((DraftSpi) draft).__show((String) prop, true);
                } else {
                    ((DraftSpi) draft).__show((PropId) prop, true);
                }
            });
            assertEquals("{}", base.toString());
            assertEquals("{\"name\":\"base\"}", shown.toString());
            assertSame(shown, BookDraft.$.produce(shown, draft -> DraftObjects.show(draft, BookProps.NAME)));
        }
    }

    @Test
    public void testBaseObjectShouldRemainUnchangedAfterCreatingDerivedObject() {

        Book original = BookDraft.$.produce(draft -> {
            draft.setName("original");
            draft.setPrice(1);
            DraftObjects.hide(draft, BookProps.NAME);
        });

        String originalState = original.toString();

        Book modified = Immutables.createBook(original, draft -> {
            draft.setName("modified");
            draft.setPrice(2);
            DraftObjects.show(draft, BookProps.NAME);
        });


        assertEquals("{\"price\":1}", originalState);
        assertEquals("{\"name\":\"modified\",\"price\":2}", modified.toString());

        assertEquals("original", original.name());
        assertEquals("modified", modified.name());
    }

    @Test
    public void testMultipleDerivedObjectsShouldNotInterfereWithEachOther() {

        Book base = BookDraft.$.produce(draft -> {
            draft.setName("base");
            draft.setPrice(1);
            DraftObjects.hide(draft, BookProps.NAME);
        });

        String baseState = base.toString();

        Book derived1 = Immutables.createBook(base, draft -> {
            draft.setName("derived1");
            DraftObjects.show(draft, BookProps.NAME);
        });

        Book derived2 = Immutables.createBook(base, draft -> {
            draft.setName("derived2");
        });

        assertEquals("{\"price\":1}", baseState);

        assertEquals("{\"name\":\"derived1\",\"price\":1}", derived1.toString());
        assertEquals("derived1", derived1.name());

        assertEquals("{\"price\":1}", derived2.toString());
        assertEquals("derived2", derived2.name());

        assertEquals("base", base.name());
        assertEquals(1, base.price());
    }

    @Test
    public void test() throws Exception {

        Book book = BookDraft.$.produce(b -> {
            b.setName("book");
            b.applyStore(s -> {
                s.setName("store");
            });
            b.addIntoAuthors(a -> {
                a.setName("Jim");
            });
            b.addIntoAuthors(a -> {
                a.setName("Kate");
            });
        });
        Book book2 = ImmutableObjects.fromString(Book.class, book.toString());

        Book book3 = BookDraft.$.produce(book, b -> {
            b.setName(b.name() + "!");
            b.store().setName(b.store().name() + "!");
            b.authors(true).forEach(a -> {
                a.setName(a.name() + "!");
            });
        });

        assertEquals(
                "{\"name\":\"book\",\"store\":{\"name\":\"STORE\"},\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                book.toString()
        );

        assertEquals(
                "{\"name\":\"book\",\"store\":{\"name\":\"STORE\"},\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                book2.toString()
        );

        assertEquals(
                "{\"name\":\"book!\",\"store\":{\"name\":\"STORE!\"},\"authors\":[{\"name\":\"Jim!\"},{\"name\":\"Kate!\"}]}",
                book3.toString()
        );
    }
}
