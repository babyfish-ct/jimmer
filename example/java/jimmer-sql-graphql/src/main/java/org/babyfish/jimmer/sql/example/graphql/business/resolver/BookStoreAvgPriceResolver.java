package org.babyfish.jimmer.sql.example.graphql.business.resolver;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.cache.Caches;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.graphql.repository.BookStoreRepository;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookProps;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreProps;
import org.babyfish.jimmer.sql.filter.Filters;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

@Component
public class BookStoreAvgPriceResolver implements TransientResolver<Long, BigDecimal> {

    private final BookStoreRepository bookStoreRepository;

    public BookStoreAvgPriceResolver(BookStoreRepository bookStoreRepository) {
        this.bookStoreRepository = bookStoreRepository;
    }

    @Override
    public Map<Long, BigDecimal> resolve(Collection<Long> ids) {
        return bookStoreRepository.findIdAndAvgBookPrice(ids)
                .stream()
                .collect(
                        Collectors.toMap(Tuple2::get_1, Tuple2::get_2)
                );
    }

    // -----------------------------
    // If you are a beginner, you can ignore all the following code.
    //
    // The following code is only used for cache mode(start the application
    // by `application-cache.yml`).
    //
    // Unlike the fully automatic cache consistency maintenance of
    // ordinary associated property, if a calculated property uses cache,
    // its consistency requires manual assistance.
    // -----------------------------

    @EventListener
    public void onAssociationChanged(AssociationEvent e) {
        if (e.getConnection() == null && e.getImmutableProp() == BookStoreProps.BOOKS.unwrap()) {
            // 1. Check whether the association `BookStore.books` is changed,
            //    this event can be caused by 2 cases:
            //    i. The foreign key `Book.store.id` is changed.
            //    ii. The `TenantFilter` is enabled and the `Book.tenant` is changed.

            Caches caches = bookStoreRepository.sql().getCaches();
            caches
                    .getPropertyCache(BookStoreProps.AVG_PRICE)
                    .delete(e.getSourceId());
        }
    }

    @EventListener
    public void onEntityChanged(EntityEvent<?> e) {
        if (e.getConnection() == null && e.getImmutableType().getJavaClass() == Book.class) {
            Ref<BookStore> storeRef = e.getUnchangedFieldRef(BookProps.STORE);
            BookStore store = storeRef != null ? storeRef.getValue() : null;
            if (store != null) { // foreign key does not change.
                // 2, Check whether `Book.price` is changed
                if (e.getChangedFieldRef(BookProps.PRICE) != null) {
                    Caches caches = bookStoreRepository.sql().getCaches();
                    caches
                            .getPropertyCache(BookStoreProps.AVG_PRICE)
                            .delete(store.id());
                }
            }
        }
    }

    // Contribute part of the secondary hash key to multiview-cache
    @Override
    public Ref<SortedMap<String, Object>> getParameterMapRef() {
        Filters filters = bookStoreRepository.sql().getFilters();
        return filters.getTargetParameterMapRef(BookStoreProps.BOOKS);
    }
}
