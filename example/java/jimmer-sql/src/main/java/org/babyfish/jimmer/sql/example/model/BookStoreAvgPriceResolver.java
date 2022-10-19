package org.babyfish.jimmer.sql.example.model;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.filter.CacheableFilter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

public class BookStoreAvgPriceResolver implements TransientResolver.Parameterized<Long, BigDecimal> {

    private final JSqlClient sqlClient;

    private final CacheableFilter<?> cacheableBooksFilter;

    public BookStoreAvgPriceResolver(JSqlClient sqlClient) {

        this.sqlClient = sqlClient;
        this.cacheableBooksFilter = sqlClient
                .getFilters()
                .getCacheableTargetFilter(BookStoreProps.BOOKS);

        // Unlike object caches and associative caches that can be automatically synchronized,
        // business computing caches require users to implement their synchronization logic.

        // 1. Check whether the association `BookStore.books` is changed
        sqlClient.getTriggers().addAssociationListener(BookStoreProps.BOOKS, e -> {
            sqlClient.getCaches().getPropertyCache(BookStoreProps.AVG_PRICE).delete(e.getSourceId());
        });
        sqlClient.getTriggers().addEntityListener(Book.class, e -> {
            Ref<BookStore> storeRef = e.getUnchangedFieldRef(BookProps.STORE);
            BookStore store = storeRef != null ? storeRef.getValue() : null;
            if (store != null) {
                // 2, Otherwise, check whether `Book.price` is changed or
                // `cacheableBooksFilter` is affected
                boolean evict = false;
                if (e.getUnchangedFieldRef(BookProps.PRICE) == null) {
                    evict = true;
                } else if (cacheableBooksFilter != null && cacheableBooksFilter.isAffectedBy(e)) {
                    evict = true;
                }
                if (evict) {
                    sqlClient
                            .getCaches()
                            .getPropertyCache(BookStoreProps.AVG_PRICE)
                            .delete(store.id());
                }
            }
        });
    }

    @Override
    public SortedMap<String, Object> getParameters() {
        return cacheableBooksFilter != null ? cacheableBooksFilter.getParameters() : null;
    }

    @Override
    public Map<Long, BigDecimal> resolve(Collection<Long> ids, Connection con) {
        List<Tuple2<Long, BigDecimal>> tuples = sqlClient
                .createQuery(BookTable.class, (q, book) -> {
                    q.where(book.store().id().in(ids));
                    q.groupBy(book.store().id());
                    return q.select(
                            book.store().id(),
                            book.price().avg()
                    );
                })
                .execute(con); // Important to specify connection
        return ensureKeys(
                Tuple2.toMap(tuples),
                ids,
                BigDecimal.ZERO
        );
    }

    private static <K, V> Map<K, V> ensureKeys(
            Map<K, V> map,
            Collection<K> keys,
            V defaultValue
    ) {
        Set<K> missedKeys = new HashSet<>(keys);
        missedKeys.removeAll(map.keySet());
        if (!missedKeys.isEmpty()) {
            for (K missedKey : missedKeys) {
                map.put(missedKey, defaultValue);
            }
        }
        return map;
    }
}
