package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeepCloneTest {

    @Test
    public void test() {
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
        Book book2 = ImmutableObjects.deepClone(book);
        Assertions.assertEquals(
                book.toString(),
                book2.toString()
        );
        Assertions.assertNotSame(book, book2);
        Assertions.assertNotSame(book.store(), book2.store());
        Assertions.assertNotSame(book.authors().get(0), book2.authors().get(0));
        Assertions.assertNotSame(book.authors().get(1), book2.authors().get(1));
    }
}
