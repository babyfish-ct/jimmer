package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.common.TestUtils;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class TriggersTest {

    private TriggersImpl triggers;

    private List<EntityEvent<Book>> bookEvents;

    private List<AssociationEvent> bookStoreEvents;

    private List<AssociationEvent> storeBookListEvents;

    private List<AssociationEvent> bookAuthorListEvents;

    private List<AssociationEvent> authorBookListEvents;

    @BeforeEach
    public void initialize() {
        triggers = new TriggersImpl();
        bookEvents = new ArrayList<>();
        bookStoreEvents = new ArrayList<>();
        storeBookListEvents = new ArrayList<>();
        bookAuthorListEvents = new ArrayList<>();
        authorBookListEvents = new ArrayList<>();
        triggers.addEntityListener(Book.class, bookEvents::add);
        triggers.addAssociationListener(BookTable.class, BookTable::store, bookStoreEvents::add);
        triggers.addAssociationListener(BookStoreTableEx.class, BookStoreTableEx::books, storeBookListEvents::add);
        triggers.addAssociationListener(BookTableEx.class, BookTableEx::authors, bookAuthorListEvents::add);
        triggers.addAssociationListener(AuthorTableEx.class, AuthorTableEx::books, authorBookListEvents::add);
    }

    @Test
    public void fireInsertBookWithNullParent() {
        triggers.fireEntityTableChange(
                null,
                (ImmutableSpi) BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("GraphQL in Action")
                            .setStore((BookStore) null);
                })
        );
        TestUtils.expect(
                "[" +
                        "--->Event{" +
                        "--->--->oldEntity=null, " +
                        "--->--->newEntity={" +
                        "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"store\":null" +
                        "--->--->}" +
                        "--->}" +
                        "]",
                bookEvents
        );
        TestUtils.expect("[]", bookStoreEvents);
        TestUtils.expect("[]", storeBookListEvents);
        TestUtils.expect("[]", bookAuthorListEvents);
        TestUtils.expect("[]", authorBookListEvents);
    }

    @Test
    public void fireInsertBook() {
        triggers.fireEntityTableChange(
                null,
                (ImmutableSpi) BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("GraphQL in Action")
                            .setStore(store -> store.setId(manningId));
                })
        );
        TestUtils.expect(
                "[" +
                        "--->Event{" +
                        "--->--->oldEntity=null, " +
                        "--->--->newEntity={" +
                        "--->--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"store\":{\"id\":\"" + manningId + "\"}" +
                        "--->--->}" +
                        "--->}" +
                        "]",
                bookEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + manningId +
                        "--->}" +
                        "]",
                bookStoreEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->--->sourceId=" + manningId + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + graphQLInActionId3  +
                        "--->}" +
                        "]",
                storeBookListEvents
        );
        TestUtils.expect("[]", bookAuthorListEvents);
        TestUtils.expect("[]", authorBookListEvents);
    }

    @Test
    public void fireDeleteBook() {
        triggers.fireEntityTableChange(
                (ImmutableSpi) BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("GraphQL in Action")
                            .setStore(store -> store.setId(manningId));
                }),
                null
        );
        TestUtils.expect(
                "[" +
                        "--->Event{" +
                        "--->--->oldEntity={" +
                        "--->--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"store\":{\"id\":\"" + manningId + "\"}" +
                        "--->--->}, " +
                        "--->--->newEntity=null" +
                        "--->}" +
                        "]",
                bookEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=" + manningId + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}" +
                        "]",
                bookStoreEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->--->sourceId=" + manningId + ", " +
                        "--->--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}" +
                        "]",
                storeBookListEvents
        );
        TestUtils.expect("[]", bookAuthorListEvents);
        TestUtils.expect("[]", authorBookListEvents);
    }

    @Test
    public void fireUpdateBook() {
        triggers.fireEntityTableChange(
                (ImmutableSpi) BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("GraphQL in Action")
                            .setStore(store -> store.setId(manningId));
                }),
                (ImmutableSpi) BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("GraphQL in Action3")
                            .setStore(store -> store.setId(oreillyId));
                })
        );
        TestUtils.expect(
                "[" +
                        "--->Event{" +
                        "--->--->oldEntity={" +
                        "--->--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->--->\"name\":\"GraphQL in Action\"," +
                        "--->--->--->\"store\":{\"id\":\"" + manningId + "\"}" +
                        "--->--->}, " +
                        "--->--->newEntity={" +
                        "--->--->--->\"id\":\"" + graphQLInActionId3 + "\"," +
                        "--->--->--->\"name\":\"GraphQL in Action3\"," +
                        "--->--->--->\"store\":{\"id\":\"" + oreillyId + "\"}" +
                        "--->--->}" +
                        "--->}" +
                        "]",
                bookEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.store, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=" + manningId + ", " +
                        "--->--->attachedTargetId=" + oreillyId +
                        "--->}" +
                        "]",
                bookStoreEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->--->sourceId=" + manningId + ", " +
                        "--->--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}, " +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->--->sourceId=" + oreillyId + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + graphQLInActionId3 +
                        "--->}" +
                        "]",
                storeBookListEvents
        );
        TestUtils.expect("[]", bookAuthorListEvents);
        TestUtils.expect("[]", authorBookListEvents);
    }

    @Test
    public void fireDeleteAssociation() {
        triggers.fireMiddleTableDelete(
                ImmutableProps.join(BookTableEx.class, BookTableEx::authors),
                graphQLInActionId3,
                danId
        );
        TestUtils.expect("[]", bookEvents);
        TestUtils.expect("[]", bookStoreEvents);
        TestUtils.expect("[]", storeBookListEvents);
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=" + danId + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}" +
                        "]",
                bookAuthorListEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->--->sourceId=" + danId + ", " +
                        "--->--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}" +
                        "]",
                authorBookListEvents
        );
    }

    @Test
    public void fireInsertAssociation() {
        triggers.fireMiddleTableInsert(
                ImmutableProps.join(BookTableEx.class, BookTableEx::authors),
                graphQLInActionId3,
                danId
        );
        TestUtils.expect("[]", bookEvents);
        TestUtils.expect("[]", bookStoreEvents);
        TestUtils.expect("[]", storeBookListEvents);
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + danId +
                        "--->}" +
                        "]",
                bookAuthorListEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->--->sourceId=" + danId + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + graphQLInActionId3 +
                        "--->}" +
                        "]",
                authorBookListEvents
        );
    }

    @Test
    public void fireDeleteInverseAssociation() {
        triggers.fireMiddleTableDelete(
                ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books),
                danId,
                graphQLInActionId3
        );
        TestUtils.expect("[]", bookEvents);
        TestUtils.expect("[]", bookStoreEvents);
        TestUtils.expect("[]", storeBookListEvents);
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=" + danId + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}" +
                        "]",
                bookAuthorListEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->--->sourceId=" + danId + ", " +
                        "--->--->detachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->attachedTargetId=null" +
                        "--->}" +
                        "]",
                authorBookListEvents
        );
    }

    @Test
    public void fireInsertInverseAssociation() {
        triggers.fireMiddleTableInsert(
                ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books),
                danId,
                graphQLInActionId3
        );
        TestUtils.expect("[]", bookEvents);
        TestUtils.expect("[]", bookStoreEvents);
        TestUtils.expect("[]", storeBookListEvents);
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Book.authors, " +
                        "--->--->sourceId=" + graphQLInActionId3 + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + danId +
                        "--->}" +
                        "]",
                bookAuthorListEvents
        );
        TestUtils.expect(
                "[" +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.Author.books, " +
                        "--->--->sourceId=" + danId + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + graphQLInActionId3 +
                        "--->}" +
                        "]",
                authorBookListEvents
        );
    }
}
