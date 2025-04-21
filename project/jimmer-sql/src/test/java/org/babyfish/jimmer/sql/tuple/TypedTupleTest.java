package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

public class TypedTupleTest extends AbstractQueryTest {

    @TypedTuple
    static class MyBookStore {
        BookStore raw;
        int denseRank;
    }

    @Test
    public void testBaseQuery() {
        BookStoreTable store = BookStoreTable.$;
        BookTable book = BookTable.$;
        TypedTupleTest_.MyBookStoreBaseTable baseTable = getSqlClient()
                .createQuery(store)
                .select(
                        TypedTupleTest_.MyBookStoreMapper.of()
                                .raw(store)
                                .denseRank(
                                        Expression.numeric().sql(
                                                Integer.class,
                                                "dense_rank() over(order by %e desc)",
                                                getSqlClient()
                                                        .createSubQuery(book)
                                                        .where(book.store().eq(store))
                                                        .selectCount()
                                        )
                                )
                )
                .asBaseTable();
        TypedRootQuery<BookStore> q = getSqlClient()
                .createQuery(baseTable)
                .where(baseTable.denseRank().le(2))
                .select(
                        baseTable.raw().fetch(
                                BookStoreFetcher.$
                                        .allScalarFields()
                                        .books(
                                                BookFetcher.$.allScalarFields()
                                        )
                        )
                );
        executeAndExpect(
                q,
                ctx -> {

                }
        );
    }
}
