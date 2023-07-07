package org.babyfish.jimmer.sql.example.graphql.business.resolver;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.graphql.entities.BookProps;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreProps;
import org.babyfish.jimmer.sql.example.graphql.repository.BookRepository;
import org.babyfish.jimmer.sql.filter.Filters;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

@Component
public class BookStoreAvgPriceResolver implements TransientResolver<Long, BigDecimal> {

    private final BookRepository bookRepository;

    private final JSqlClient sqlClient;

    public BookStoreAvgPriceResolver(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        this.sqlClient = bookRepository.sql(); // You can also inject it directly
    }

    @Override
    public Map<Long, BigDecimal> resolve(Collection<Long> ids) {
        return bookRepository.findAvgPriceGroupByStoreId(ids);
    }

    @Override
    public BigDecimal getDefaultValue() {
        return BigDecimal.ZERO;
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
                    .getPropertyCache(BookStoreProps.AVG_PRICE)
                    .delete(e.getSourceId());
        }
    }

    @EventListener
    public void onEntityChanged(EntityEvent<?> e) {
        // The scalar property `Book.price` is changed
        if (sqlClient.getCaches().isAffectedBy(e) && e.isChanged(BookProps.PRICE)) {
            Ref<BookStore> storeRef = e.getUnchangedRef(BookProps.STORE);
            BookStore store = storeRef != null ? storeRef.getValue() : null;
            if (store != null) {
                sqlClient
                        .getCaches()
                        .getPropertyCache(BookStoreProps.AVG_PRICE)
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