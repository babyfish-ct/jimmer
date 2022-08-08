package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.BookDraft;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class EntityEventTest {

    private static final int NAME_PROP_ID =
            ImmutableProps.get(BookTable.class, BookTable::name).getId();

    private static final int STORE_PROP_ID =
            ImmutableProps.join(BookTable.class, BookTable::store).getId();

    @Test
    public void testDeleteScalar() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("InsertBookName");
                }),
                null,
                null
        );
        Assertions.assertEquals(
                "InsertBookName",
                event.getUnchangedFieldRef(NAME_PROP_ID).getValue()
        );
    }
    
    @Test
    public void testInsertScalar() {
        EntityEvent<Book> event = new EntityEvent<>(
                null,
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("InsertBookName");
                }),
                null
        );
        Assertions.assertEquals(
                "InsertBookName",
                event.getUnchangedFieldRef(NAME_PROP_ID).getValue()
        );
    }

    @Test
    public void testDeleteNullReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore((BookStore) null);
                }),
                null,
                null
        );
        Assertions.assertNull(
                event.getUnchangedFieldRef(STORE_PROP_ID).getValue()
        );
    }

    @Test
    public void testDeleteNonNullReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(manningId));
                }),
                null,
                null
        );
        Assertions.assertEquals(
                manningId,
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID).getValue().id()
        );
    }

    @Test
    public void testInsertNullReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                null,
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore((BookStore) null);
                }),
                null
        );
        Assertions.assertNull(
                event.getUnchangedFieldRef(STORE_PROP_ID).getValue()
        );
    }

    @Test
    public void testInsertNonNullReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                null,
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(manningId));
                }),
                null
        );
        Assertions.assertEquals(
                manningId,
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID).getValue().id()
        );
    }

    @Test
    public void testNotUpdateReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(manningId));
                }),
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(manningId));
                }),
                null
        );
        Assertions.assertEquals(
                manningId,
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID).getValue().id()
        );
    }

    @Test
    public void testNotUpdateNullReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore((BookStore) null);
                }),
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore((BookStore) null);
                }),
                null
        );
        Assertions.assertEquals(
                null,
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID).getValue()
        );
    }

    @Test
    public void testUpdateReference() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(manningId));
                }),
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(oreillyId));
                }),
                null
        );
        Assertions.assertNull(
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID)
        );
    }

    @Test
    public void testUpdateReferenceToNull() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(manningId));
                }),
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore((BookStore) null);
                }),
                null
        );
        Assertions.assertNull(
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID)
        );
    }

    @Test
    public void testUpdateReferenceToNonNull() {
        EntityEvent<Book> event = new EntityEvent<>(
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore((BookStore) null);
                }),
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setStore(store -> store.setId(oreillyId));
                }),
                null
        );
        Assertions.assertNull(
                event.<BookStore>getUnchangedFieldRef(STORE_PROP_ID)
        );
    }
}
