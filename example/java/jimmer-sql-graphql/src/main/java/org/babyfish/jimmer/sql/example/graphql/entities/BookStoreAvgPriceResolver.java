package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.fluent.Fluent;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

public class BookStoreAvgPriceResolver implements TransientResolver<Long, BigDecimal> {

    private static final ImmutableProp BOOK_DOT_STORE =
            ImmutableProps.join(BookTable.class, BookTable::store);

    private static final ImmutableProp BOOK_DOT_PRICE =
            ImmutableProps.get(BookTable.class, BookTable::price);

    private static final ImmutableProp BOOK_STORE_DOT_AVG_PRICE =
            ImmutableType.get(BookStore.class).getProp("avgPrice");

    private final JSqlClient sqlClient;

    public BookStoreAvgPriceResolver(JSqlClient sqlClient) {

        this.sqlClient = sqlClient;

        // Unlike object caches and associative caches that can be automatically synchronized,
        // business computing caches require users to implement their synchronization logic.

        // 1. Check whether the association `BookStore.books` is changed
        sqlClient.getTriggers().addAssociationListener(BookStoreProps.BOOKS, e -> {
            sqlClient.getCaches().getPropertyCache(BookStoreProps.AVG_PRICE).delete(e.getSourceId());
        });
        sqlClient.getTriggers().addEntityListener(Book.class, e -> {
            Ref<BookStore> storeRef = e.getUnchangedFieldRef(BookProps.STORE);
            if (storeRef != null && storeRef.getValue() != null) {
                // 2, Otherwise, check whether `Book.price` is changed
                if (e.getUnchangedFieldRef(BookProps.PRICE) == null) {
                    sqlClient.getCaches().getPropertyCache(BookStoreProps.AVG_PRICE).delete(storeRef.getValue().id());
                }
            }
        });
    }

    @Override
    public Map<Long, BigDecimal> resolve(Collection<Long> ids, Connection con) {
        Fluent fluent = sqlClient.createFluent();
        BookTable book = new BookTable();
        List<Tuple2<Long, BigDecimal>> tuples = fluent
                .query(book)
                .where(book.store().id().in(ids))
                .groupBy(book.store().id())
                .select(
                        book.store().id(),
                        book.price().avg()
                )
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
