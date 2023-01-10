package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BookTest {

    @Test
    public void test() throws JsonProcessingException {

        Book book = BookDraft.$.produce(b -> {
            b.setName("book");
            b.setStore(s -> {
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

        Assertions.assertEquals(
                "{\"name\":\"book\",\"store\":{\"name\":\"STORE\"},\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                book.toString()
        );

        Assertions.assertEquals(
                "{\"name\":\"book\",\"store\":{\"name\":\"STORE\"},\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                book2.toString()
        );

        Assertions.assertEquals(
                "{\"name\":\"book!\",\"store\":{\"name\":\"STORE!\"},\"authors\":[{\"name\":\"Jim!\"},{\"name\":\"Kate!\"}]}",
                book3.toString()
        );
    }
}
