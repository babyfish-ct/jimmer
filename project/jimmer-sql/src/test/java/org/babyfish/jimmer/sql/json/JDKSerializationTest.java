package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.DraftObjects;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class JDKSerializationTest {

    @Test
    public void testByRaw() {
        Book book = BookDraft.$.produce(draft -> {
            draft.applyStore(store -> store.setId(UUID.fromString("916e4629-f18f-49cf-9c0a-c161383d3939")));
            draft.addIntoAuthors(author -> author.setId(UUID.fromString("a3a529b5-b310-4af1-883d-9a4e0114653c")));
            draft.addIntoAuthors(author -> author.setId(UUID.fromString("1639d9d5-7b92-43cf-a03f-25314832f794")));
        });
        Assertions.assertEquals(
                "{" +
                        "\"store\":{\"id\":\"916e4629-f18f-49cf-9c0a-c161383d3939\"}," +
                        "\"authors\":[" +
                        "{\"id\":\"a3a529b5-b310-4af1-883d-9a4e0114653c\"}," +
                        "{\"id\":\"1639d9d5-7b92-43cf-a03f-25314832f794\"}" +
                        "]" +
                        "}",
                cloneBySerialization(book).toString()
        );
    }

    @Test
    public void testByView() {
        Book book = BookDraft.$.produce(draft -> {
            draft.setStoreId(UUID.fromString("916e4629-f18f-49cf-9c0a-c161383d3939"));
            draft.authorIds(true).add(UUID.fromString("a3a529b5-b310-4af1-883d-9a4e0114653c"));
            draft.authorIds(true).add(UUID.fromString("1639d9d5-7b92-43cf-a03f-25314832f794"));
            DraftObjects.hide(draft, BookProps.STORE);
            DraftObjects.hide(draft, BookProps.AUTHORS);
            DraftObjects.show(draft, BookProps.STORE_ID);
            DraftObjects.show(draft, BookProps.AUTHOR_IDS);
        });
        Assertions.assertEquals(
                "{" +
                        "\"storeId\":\"916e4629-f18f-49cf-9c0a-c161383d3939\"," +
                        "\"authorIds\":[" +
                        "\"a3a529b5-b310-4af1-883d-9a4e0114653c\"," +
                        "\"1639d9d5-7b92-43cf-a03f-25314832f794\"" +
                        "]" +
                        "}",
                cloneBySerialization(book).toString()
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T cloneBySerialization(T value) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
                out.writeObject(value);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (Exception ex) {
            throw Assertions.<RuntimeException>fail(ex);
        }
    }
}
