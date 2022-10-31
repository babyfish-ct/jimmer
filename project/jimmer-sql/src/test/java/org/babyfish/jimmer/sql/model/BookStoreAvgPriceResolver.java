package org.babyfish.jimmer.sql.model;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

public class BookStoreAvgPriceResolver implements TransientResolver<UUID, BigDecimal> {

    private final JSqlClient sqlClient;

    public BookStoreAvgPriceResolver(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public Map<UUID, BigDecimal> resolve(Collection<UUID> ids, Connection con) {
        BookTable book = BookTable.$;
        List<Tuple2<UUID, BigDecimal>> tuples =
                sqlClient.createQuery(book)
                        .where(book.store().id().in(ids))
                        .groupBy(book.store().id())
                        .select(
                                book.store().id(),
                                book.price().avg().coalesce(BigDecimal.ZERO)
                        )
                        .execute(con);
        return Tuple2.toMap(tuples);
    }
}
