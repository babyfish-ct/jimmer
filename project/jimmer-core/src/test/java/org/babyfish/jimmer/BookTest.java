package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.babyfish.jimmer.model.BookProps;
import org.babyfish.jimmer.model.Immutables;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class BookTest {

    @JacksonTest
    public void testVisibilityOnlyChangesDoNotMutateBase(JacksonBackend backend) {
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
            assertEquals("{}", backend.write(base));
            assertEquals("{\"name\":\"base\"}", backend.write(shown));
            assertSame(shown, BookDraft.$.produce(shown, draft -> DraftObjects.show(draft, BookProps.NAME)));
        }
    }

    @JacksonTest
    public void testBaseObjectShouldRemainUnchangedAfterCreatingDerivedObject(JacksonBackend backend) {

        Book original = BookDraft.$.produce(draft -> {
            draft.setName("original");
            draft.setPrice(1);
            DraftObjects.hide(draft, BookProps.NAME);
        });

        String originalState = backend.write(original);

        Book modified = Immutables.createBook(original, draft -> {
            draft.setName("modified");
            draft.setPrice(2);
            DraftObjects.show(draft, BookProps.NAME);
        });


        assertEquals("{\"price\":1}", originalState);
        assertEquals("{\"name\":\"modified\",\"price\":2}", backend.write(modified));

        assertEquals("original", original.name());
        assertEquals("modified", modified.name());
    }

    @JacksonTest
    public void testMultipleDerivedObjectsShouldNotInterfereWithEachOther(JacksonBackend backend) {

        Book base = BookDraft.$.produce(draft -> {
            draft.setName("base");
            draft.setPrice(1);
            DraftObjects.hide(draft, BookProps.NAME);
        });

        String baseState = backend.write(base);

        Book derived1 = Immutables.createBook(base, draft -> {
            draft.setName("derived1");
            DraftObjects.show(draft, BookProps.NAME);
        });

        Book derived2 = Immutables.createBook(base, draft -> {
            draft.setName("derived2");
        });

        assertEquals("{\"price\":1}", baseState);

        assertEquals("{\"name\":\"derived1\",\"price\":1}", backend.write(derived1));
        assertEquals("derived1", derived1.name());

        assertEquals("{\"price\":1}", backend.write(derived2));
        assertEquals("derived2", derived2.name());

        assertEquals("base", base.name());
        assertEquals(1, base.price());
    }

    @JacksonTest
    public void test(JacksonBackend backend) throws Exception {

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
        Book book2 = backend.read(Book.class, backend.write(book));

        Book book3 = BookDraft.$.produce(book, b -> {
            b.setName(b.name() + "!");
            b.store().setName(b.store().name() + "!");
            b.authors(true).forEach(a -> {
                a.setName(a.name() + "!");
            });
        });

        assertEquals(
                "{\"name\":\"book\",\"store\":{\"name\":\"STORE\"},\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                backend.write(book)
        );

        assertEquals(
                "{\"name\":\"book\",\"store\":{\"name\":\"STORE\"},\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                backend.write(book2)
        );

        assertEquals(
                "{\"name\":\"book!\",\"store\":{\"name\":\"STORE!\"},\"authors\":[{\"name\":\"Jim!\"},{\"name\":\"Kate!\"}]}",
                backend.write(book3)
        );
    }
}
