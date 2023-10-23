package org.babyfish.jimmer.sql.example.graphql.business.resolver;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.example.graphql.entities.Book;
import org.babyfish.jimmer.sql.example.graphql.entities.BookProps;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStore;
import org.babyfish.jimmer.sql.example.graphql.entities.BookStoreProps;
import org.babyfish.jimmer.sql.example.graphql.repository.BookRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
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




    // The calculated property `BookStore.avgPrice` depends on the one-to-many association `BookStore.books`,
    // and `BookStore.books` adopts multi-view cache because its target type `Book` is processed by the
    // filter `TenantFilter`, so `BookStore.avgPrice` should also adopt multi-view cache too.
    //
    // Since it is multi-view cache, sub key is needed.
    // Here, we make the calculated cache `BookStore.avgPrice` have the same sub key as the
    // association cache `BookStore.books`, which is `{"tenant": ...}`
    @Override
    public Ref<SortedMap<String, Object>> getParameterMapRef() { // ❹
        return sqlClient.getFilters().getTargetParameterMapRef(BookStoreProps.BOOKS);
    }

    // When a one-to-many association `BookStore.books` is modified
    // (for some records in the BOOK table, whether by modifying the foreign key field `STORE_ID` or
    // the field `TENANT` that the `TenantFilter` cares about),
    // the cache of the calculated property `BookStore.avgPrice` should be invalidated.
    @Override
    public Collection<?> getAffectedSourceIds(@NotNull AssociationEvent e) { // ❺
        if (sqlClient.getCaches().isAffectedBy(e) && e.getImmutableProp() == BookStoreProps.BOOKS.unwrap()) {
            return Collections.singletonList(e.getSourceId());
        }
        return null;
    }

    // Given that the foreign key `STORE_ID` of the current `Book` is not null and has not been modified,
    // if the `price` of the current `Book` changes, the cache of the computed property `BookStore.avgPrice`
    // corresponding to `STORE_ID` should be invalidated.
    @Override
    public Collection<?> getAffectedSourceIds(@NotNull EntityEvent<?> e) { // ❻
        if (sqlClient.getCaches().isAffectedBy(e) &&
                !e.isEvict() &&
                e.getImmutableType().getJavaClass() == Book.class) {

            Ref<BookStore> storeRef = e.getUnchangedRef(BookProps.STORE);
            if (storeRef != null && storeRef.getValue() != null && e.isChanged(BookProps.PRICE)) {
                return Collections.singletonList(storeRef.getValue().id());
            }
        }
        return null;
    }
}

/*----------------Documentation Links----------------
❶ ❷ ❸ https://babyfish-ct.github.io/jimmer/docs/mapping/advanced/calculated/transient#scalar-calculation-bookstoreavgprice
❹ https://babyfish-ct.github.io/jimmer/docs/cache/multiview-cache/user-filter#subkey-of-calculated-properties
❺ ❻ https://babyfish-ct.github.io/jimmer/docs/cache/multiview-cache/user-filter#consistency
---------------------------------------------------*/
