package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

public class DeepCloneTest {

    @JacksonTest
    public void test(JacksonBackend backend) {
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
                backend.write(book),
                backend.write(book2)
        );
        Assertions.assertNotSame(book, book2);
        Assertions.assertNotSame(book.store(), book2.store());
        Assertions.assertNotSame(book.authors().get(0), book2.authors().get(0));
        Assertions.assertNotSame(book.authors().get(1), book2.authors().get(1));
    }
}
