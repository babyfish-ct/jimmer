package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.common.TestUtils;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Assertions;
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

    private EntityListener<Book> bookHandler;

    private AssociationListener bookStoreHandler;

    private AssociationListener storeBookListHandler;

    private AssociationListener bookAuthorListHandler;

    private AssociationListener authorBookListHandler;

    @BeforeEach
    public void initialize() {

        triggers = new TriggersImpl();
        bookEvents = new ArrayList<>();
        bookStoreEvents = new ArrayList<>();
        storeBookListEvents = new ArrayList<>();
        bookAuthorListEvents = new ArrayList<>();
        authorBookListEvents = new ArrayList<>();

        bookHandler = bookEvents::add;
        bookStoreHandler = bookStoreEvents::add;
        storeBookListHandler = storeBookListEvents::add;
        bookAuthorListHandler = bookAuthorListEvents::add;
        authorBookListHandler = authorBookListEvents::add;

        triggers.addEntityListener(Book.class, bookHandler);
        triggers.addAssociationListener(BookTable.class, BookTable::store, bookStoreHandler);
        triggers.addAssociationListener(BookStoreTableEx.class, BookStoreTableEx::books, storeBookListHandler);
        triggers.addAssociationListener(BookTableEx.class, BookTableEx::authors, bookAuthorListHandler);
        triggers.addAssociationListener(AuthorTableEx.class, AuthorTableEx::books, authorBookListHandler);
    }

    @Test
    public void testRemoveListener() {

        Assertions.assertTrue(triggers.hasListeners(ImmutableType.get(Book.class)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTable.class, BookTable::store)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookStoreTableEx.class, BookStoreTableEx::books)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTableEx.class, BookTableEx::authors)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books)));

        triggers.removeEntityListener(Book.class, bookHandler);
        Assertions.assertTrue(triggers.hasListeners(ImmutableType.get(Book.class)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTable.class, BookTable::store)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookStoreTableEx.class, BookStoreTableEx::books)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTableEx.class, BookTableEx::authors)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books)));

        triggers.removeAssociationListener(BookTable.class, BookTable::store, bookStoreHandler);
        Assertions.assertTrue(triggers.hasListeners(ImmutableType.get(Book.class)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTable.class, BookTable::store)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookStoreTableEx.class, BookStoreTableEx::books)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTableEx.class, BookTableEx::authors)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books)));

        triggers.removeAssociationListener(BookStoreTableEx.class, BookStoreTableEx::books, storeBookListHandler);
        Assertions.assertFalse(triggers.hasListeners(ImmutableType.get(Book.class)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookTable.class, BookTable::store)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookStoreTableEx.class, BookStoreTableEx::books)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTableEx.class, BookTableEx::authors)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books)));

        triggers.removeAssociationListener(BookTableEx.class, BookTableEx::authors, bookAuthorListHandler);
        Assertions.assertFalse(triggers.hasListeners(ImmutableType.get(Book.class)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookTable.class, BookTable::store)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookStoreTableEx.class, BookStoreTableEx::books)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(BookTableEx.class, BookTableEx::authors)));
        Assertions.assertTrue(triggers.hasListeners(ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books)));

        triggers.removeAssociationListener(AuthorTableEx.class, AuthorTableEx::books, authorBookListHandler);
        Assertions.assertFalse(triggers.hasListeners(ImmutableType.get(Book.class)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookTable.class, BookTable::store)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookStoreTableEx.class, BookStoreTableEx::books)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(BookTableEx.class, BookTableEx::authors)));
        Assertions.assertFalse(triggers.hasListeners(ImmutableProps.join(AuthorTableEx.class, AuthorTableEx::books)));
    }

    @Test
    public void fireInsertBookWithNullParent() {
        triggers.fireEntityTableChange(
                null,
                BookDraft.$.produce(book -> {
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
                        "--->--->}, " +
                        "--->--->reason=null" +
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
                BookDraft.$.produce(book -> {
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
                        "--->--->}, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + manningId + ", " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + graphQLInActionId3  + ", " +
                        "--->--->reason=null" +
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
                BookDraft.$.produce(book -> {
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
                        "--->--->newEntity=null, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
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
                BookDraft.$.produce(book -> {
                    book
                            .setId(graphQLInActionId3)
                            .setName("GraphQL in Action")
                            .setStore(store -> store.setId(manningId));
                }),
                BookDraft.$.produce(book -> {
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
                        "--->--->}, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + oreillyId + ", " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
                        "--->}, " +
                        "--->AssociationEvent{" +
                        "--->--->prop=org.babyfish.jimmer.sql.model.BookStore.books, " +
                        "--->--->sourceId=" + oreillyId + ", " +
                        "--->--->detachedTargetId=null, " +
                        "--->--->attachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + danId + ", " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=null, " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + danId + ", " +
                        "--->--->reason=null" +
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
                        "--->--->attachedTargetId=" + graphQLInActionId3 + ", " +
                        "--->--->reason=null" +
                        "--->}" +
                        "]",
                authorBookListEvents
        );
    }
}
