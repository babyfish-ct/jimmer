package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.model.Author;
import org.babyfish.jimmer.model.AuthorDraft;
import org.babyfish.jimmer.model.Book;
import org.babyfish.jimmer.model.BookDraft;
import org.babyfish.jimmer.runtime.Internal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ContextTest {

    @Test
    public void testRequired() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setName("SQL in Action");
            Author author1 = AuthorDraft.$.produce(a -> a.setName("Jim"));
            Author author2 = AuthorDraft.$.produce(a -> a.setName("Kate"));
            Assertions.assertInstanceOf(Draft.class, author1);
            Assertions.assertInstanceOf(Draft.class, author2);
            draft.setAuthors(Arrays.asList(author1, author2));
        });
        Assertions.assertEquals(
                "{\"name\":\"SQL in Action\",\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                book.toString()
        );
    }

    @Test
    public void testRequiresNew() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setName("SQL in Action");
            Author author1 = AuthorDraft.$.produce(true, a -> a.setName("Jim"));
            Author author2 = AuthorDraft.$.produce(true, a -> a.setName("Kate"));
            Assertions.assertFalse(author1 instanceof Draft);
            Assertions.assertFalse(author2 instanceof Draft);
            draft.setAuthors(Arrays.asList(author1, author2));
        });
        Assertions.assertEquals(
                "{\"name\":\"SQL in Action\",\"authors\":[{\"name\":\"Jim\"},{\"name\":\"Kate\"}]}",
                book.toString()
        );
    }
}
