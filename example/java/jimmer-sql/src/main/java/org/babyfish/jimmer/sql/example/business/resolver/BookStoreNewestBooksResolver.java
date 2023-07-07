package org.babyfish.jimmer.sql.example.business.resolver;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.repository.BookRepository;
import org.babyfish.jimmer.sql.example.model.BookProps;
import org.babyfish.jimmer.sql.example.model.BookStore;
import org.babyfish.jimmer.sql.example.model.BookStoreProps;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BookStoreNewestBooksResolver implements TransientResolver<Long, List<Long>> {

    private final BookRepository bookRepository;

    private final JSqlClient sqlClient;

    public BookStoreNewestBooksResolver(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        this.sqlClient = bookRepository.sql(); // You can also inject it directly
    }

    @Override
    public Map<Long, List<Long>> resolve(Collection<Long> ids) {
        return bookRepository.findNewestIdsGroupByStoreId(ids);
    }

    @Override
    public List<Long> getDefaultValue() {
        return Collections.emptyList();
    }

    // -----------------------------
    // If you are a beginner, you can ignore all the following code.
    //
    // The following code is only used for cache mode(start the application
    // by `application.yml`).
    //
    // Unlike the fully automatic cache consistency maintenance of
    // ordinary associated property, if a calculated property uses cache,
    // its consistency requires manual assistance.
    // -----------------------------

    @EventListener
    public void onAssociationChanged(AssociationEvent e) {
        // The association property `BookStore.books` is changed
        //
        // It is worth noting that
        // not only modifying the `STORE_ID` field of the `BOOK` table can trigger the event,
        // but also modifying the `TENANT` field of the BOOK table can trigger the event.
        if (sqlClient.getCaches().isAffectedBy(e) && e.isChanged(BookStoreProps.BOOKS)) {
            sqlClient
                    .getCaches()
                    .getPropertyCache(BookStoreProps.NEWEST_BOOKS)
                    .delete(e.getSourceId());
        }
    }

    @EventListener
    public void onEntityChanged(EntityEvent<?> e) {
        // The scalar property `Book.edition` is changed.
        if (sqlClient.getCaches().isAffectedBy(e) && e.isChanged(BookProps.EDITION)) {
            Ref<BookStore> storeRef = e.getUnchangedRef(BookProps.STORE);
            BookStore store = storeRef != null ? storeRef.getValue() : null;
            if (store != null) { // foreign key does not change.
                sqlClient
                        .getCaches()
                        .getPropertyCache(BookStoreProps.NEWEST_BOOKS)
                        .delete(store.id());
            }
        }
    }

    // Contribute part of the secondary hash key to multiview-cache
    @Override
    public Ref<SortedMap<String, Object>> getParameterMapRef() {
        return sqlClient.getFilters().getTargetParameterMapRef(BookStoreProps.BOOKS);
    }
}
