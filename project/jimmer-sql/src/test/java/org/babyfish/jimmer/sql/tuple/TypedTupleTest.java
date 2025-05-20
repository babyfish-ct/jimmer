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
                .where(baseTable.raw().name().like("M"))
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
                    ctx.sql( // aggregation
                            "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 " +
                                    "from (" +
                                    "--->select tb_4_.ID c1, tb_4_.NAME c2, tb_4_.WEBSITE c3, tb_4_.VERSION c4, " +
                                    "--->dense_rank() over(" +
                                    "--->--->order by (select count(1) from BOOK tb_2_ where tb_2_.STORE_ID = tb_4_.ID) desc" +
                                    "--->) c5 " +
                                    "--->from BOOK_STORE tb_4_" +
                                    ") tb_1_ " +
                                    "where tb_1_.c5 <= ? and tb_1_.c2 like ?"
                    );
                    ctx.statement(1).sql( // associated objects
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE " +
                                    "from BOOK tb_1_ where tb_1_.STORE_ID = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->\"name\":\"MANNING\"," +
                                    "--->\"website\":null," +
                                    "--->\"version\":0," +
                                    "--->\"books\":[" +
                                    "--->--->{" +
                                    "--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":1," +
                                    "--->--->--->\"price\":80.00" +
                                    "--->--->},{" +
                                    "--->--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":2," +
                                    "--->--->--->\"price\":81.00" +
                                    "--->--->},{" +
                                    "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":3," +
                                    "--->--->--->\"price\":80.00" +
                                    "--->--->}" +
                                    "--->]" +
                                    "}]"
                    );
                }
        );
    }
}
