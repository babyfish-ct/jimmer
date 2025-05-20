package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.TypedTuple;
import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
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

    @TypedTuple
    static class MyBook {
        Book raw;
        long authorCount;
    }

    @Test
    public void testBaseQueryWithFetch() {
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
                    ctx.sql( // aggregate-root
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

    @Test
    public void testBaseQueryWithJoinFetch() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        TypedTupleTest_.MyBookBaseTable myBook = getSqlClient()
                .createQuery(table)
                .select(
                        TypedTupleTest_.MyBookMapper.of()
                                .raw(table)
                                .authorCount(
                                        getSqlClient().createSubQuery(author)
                                                .where(author.books().id().eq(table.id()))
                                                .select(Expression.rowCount())
                                )
                )
                .asBaseTable();
        executeAndExpect(
                getSqlClient().createQuery(myBook)
                        .where(myBook.authorCount().gt(1L))
                        .select(
                                myBook.raw().fetch(
                                        BookFetcher.$.allScalarFields()
                                                .store(
                                                        ReferenceFetchType.JOIN_ALWAYS,
                                                        BookStoreFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, " +
                                    "tb_1_.c5, tb_1_.c6, tb_1_.c7, tb_1_.c8 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_5_.ID c1, tb_5_.NAME c2, tb_5_.EDITION c3, tb_5_.PRICE c4, " +
                                    "--->--->tb_6_.ID c5, tb_6_.NAME c6, tb_6_.WEBSITE c7, tb_6_.VERSION c8, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_2_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->--->--->where tb_3_.BOOK_ID = tb_5_.ID" +
                                    "--->--->) c9 " +
                                    "--->from BOOK tb_5_ " +
                                    "--->--->left join BOOK_STORE tb_6_ on tb_5_.STORE_ID = tb_6_.ID" + // join fetch
                                    ") tb_1_ " +
                                    "where tb_1_.c9 > ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"edition\":1," +
                                    "--->\"price\":50.00," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "},{" +
                                    "--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"edition\":2," +
                                    "--->\"price\":55.00," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "},{" +
                                    "--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":51.00," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "}]"
                    );
                }
        );
    }
}
