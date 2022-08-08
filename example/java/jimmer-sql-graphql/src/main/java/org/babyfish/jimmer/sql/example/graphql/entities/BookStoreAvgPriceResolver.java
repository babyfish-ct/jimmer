package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        sqlClient.getTriggers().addAssociationListener(BookStoreTableEx.class, BookStoreTableEx::books, e -> {
            sqlClient.getCaches().getPropertyCache(BOOK_STORE_DOT_AVG_PRICE).delete(e.getSourceId());
        });
        sqlClient.getTriggers().addEntityListener(Book.class, e -> {
            Ref<BookStore> storeRef = e.getUnchangedFieldRef(BOOK_DOT_STORE.getId());
            if (storeRef != null && storeRef.getValue() != null) {
                // 2, Otherwise, check whether `Book.price` is changed
                if (e.getUnchangedFieldRef(BOOK_DOT_PRICE.getId()) == null) {
                    sqlClient.getCaches().getPropertyCache(BOOK_STORE_DOT_AVG_PRICE).delete(storeRef.getValue().id());
                }
            }
        });
    }

    @Override
    public Map<Long, BigDecimal> resolve(Collection<Long> ids, Connection con) {
        List<Tuple2<Long, BigDecimal>> tuples = sqlClient
                .createQuery(BookTable.class, (q, book) -> {
                    q.where(book.store().id().in(ids));
                    q.groupBy(book.store().id());
                    return q.select(
                            book.store().id(),
                            book.price().avg().coalesce(BigDecimal.ZERO)
                    );
                })
                .execute(con); // Important to specify connection
        return Tuple2.toMap(tuples);
    }
}
