package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.fetcher.ReferenceFetchType;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.RectFetcher;
import org.babyfish.jimmer.sql.model.embedded.TransformFetcher;
import org.babyfish.jimmer.sql.model.embedded.TransformTable;
import org.junit.jupiter.api.Test;

public class BaseQueryTest extends AbstractQueryTest {

    @Test
    public void testBaseQueryWithFetch() {
        BookStoreTable store = BookStoreTable.$;
        BookTable book = BookTable.$;
        BaseTable2<BookStoreTable, NumericExpression<Integer>> baseTable = getSqlClient()
                .createBaseQuery(store)
                .addSelect(store)
                .addSelect(
                        Expression.numeric().sql(
                                Integer.class,
                                "dense_rank() over(order by %e desc)",
                                getSqlClient()
                                        .createSubQuery(book)
                                        .where(book.store().eq(store))
                                        .selectCount()
                        )
                )
                .asBaseTable();
        TypedRootQuery<BookStore> q = getSqlClient()
                .createQuery(baseTable)
                .where(baseTable.get_2().le(2))
                .where(baseTable.get_1().name().like("M"))
                .select(
                        baseTable.get_1().fetch(
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
                                    "--->select tb_2_.ID c1, tb_2_.NAME c2, tb_2_.WEBSITE c3, tb_2_.VERSION c4, " +
                                    "--->dense_rank() over(" +
                                    "--->--->order by (select count(1) from BOOK tb_3_ where tb_3_.STORE_ID = tb_2_.ID) desc" +
                                    "--->) c5 " +
                                    "--->from BOOK_STORE tb_2_" +
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
        BaseTable2<BookTable, NumericExpression<Long>> baseTable = getSqlClient()
                .createBaseQuery(table)
                .addSelect(table)
                .addSelect(
                        getSqlClient().createSubQuery(author)
                                .where(author.books().id().eq(table.id()))
                                .select(Expression.rowCount())
                )
                .asBaseTable();
        executeAndExpect(
                getSqlClient().createQuery(baseTable)
                        .where(baseTable.get_2().gt(1L))
                        .select(
                                baseTable.get_1().fetch(
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
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, " +
                                    "--->tb_1_.c5, tb_1_.c6, tb_1_.c7, tb_1_.c8 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, " +
                                    "--->--->tb_3_.ID c5, tb_3_.NAME c6, tb_3_.WEBSITE c7, tb_3_.VERSION c8, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) from AUTHOR tb_4_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ " +
                                    "--->--->--->--->on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where tb_5_.BOOK_ID = tb_2_.ID" +
                                    "--->--->) c9 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->left join BOOK_STORE tb_3_ on tb_2_.STORE_ID = tb_3_.ID" +
                                    ") tb_1_ where tb_1_.c9 > ?"
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

    @Test
    public void testBaseJoinedTableWithTable() {
        BookStoreTable store = BookStoreTable.$;
        AuthorTableEx authorEx = AuthorTableEx.$;
        BaseTable2<BookTable, NumericExpression<Long>> baseTable =
                getSqlClient()
                        .createBaseQuery(store)
                        .where(store.name().eq("MANNING"))
                        .where(store.asTableEx().books().edition().eq(3))
                        .addSelect((BookTable)store.asTableEx().books())
                        .addSelect(
                                getSqlClient().createSubQuery(authorEx)
                                        .where(authorEx.books().id().eq(store.asTableEx().books().id()))
                                        .select(Expression.rowCount())
                        )
                .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_2().gt(0L))
                        .select(baseTable.get_1()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5 " +
                                    "from (" +
                                    "--->select tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, tb_3_.STORE_ID c5, " +
                                    "--->(" +
                                    "--->--->select count(1) " +
                                    "--->--->from AUTHOR tb_4_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ " +
                                    "--->--->--->on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->where tb_5_.BOOK_ID = tb_3_.ID" +
                                    "--->) c6 " +
                                    "--->from BOOK_STORE tb_2_ " +
                                    "--->inner join BOOK tb_3_ " +
                                    "--->--->on tb_2_.ID = tb_3_.STORE_ID " +
                                    "--->where tb_2_.NAME = ? and tb_3_.EDITION = ?" +
                                    ") tb_1_ where tb_1_.c6 > ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":80.00," +
                                    "--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testBaseJoinedTableWithJoinFetch() {
        BookStoreTable store = BookStoreTable.$;
        AuthorTableEx authorEx = AuthorTableEx.$;
        BaseTable2<BookTable, NumericExpression<Long>> baseTable =
                getSqlClient()
                        .createBaseQuery(store)
                        .where(store.name().eq("MANNING"))
                        .where(store.asTableEx().books().edition().eq(3))
                        .addSelect((BookTable)store.asTableEx().books())
                        .addSelect(
                                getSqlClient().createSubQuery(authorEx)
                                        .where(authorEx.books().id().eq(store.asTableEx().books().id()))
                                        .select(Expression.rowCount())
                        )
                        .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_2().gt(0L))
                        .select(
                                baseTable.get_1().fetch(
                                        BookFetcher.$.allScalarFields()
                                                .store(
                                                        ReferenceFetchType.JOIN_ALWAYS,
                                                        BookStoreFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, tb_1_.c6, tb_1_.c7, tb_1_.c8 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_3_.ID c1, tb_3_.NAME c2, tb_3_.EDITION c3, tb_3_.PRICE c4, " +
                                    "--->--->tb_4_.ID c5, tb_4_.NAME c6, tb_4_.WEBSITE c7, tb_4_.VERSION c8, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_5_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_6_ " +
                                    "--->--->--->--->on tb_5_.ID = tb_6_.AUTHOR_ID " +
                                    "--->--->--->where tb_6_.BOOK_ID = tb_3_.ID" +
                                    "--->--->) c9 " +
                                    "--->from BOOK_STORE tb_2_ " +
                                    "--->inner join BOOK tb_3_ " +
                                    "--->--->on tb_2_.ID = tb_3_.STORE_ID " +
                                    "--->left join BOOK_STORE tb_4_ " +
                                    "--->--->on tb_3_.STORE_ID = tb_4_.ID " +
                                    "--->where tb_2_.NAME = ? and tb_3_.EDITION = ?" +
                                    ") tb_1_ " +
                                    "where tb_1_.c9 > ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":80.00," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testMergedBaseQueryWithTable() {
        BookTable book = BookTable.$;
        BookStoreTable store = BookStoreTable.$;
        AuthorTableEx authorEx = AuthorTableEx.$;
        BaseTable2<BookTable, NumericExpression<Long>> baseTable =
                TypedBaseQuery.unionAll(
                        getSqlClient()
                                .createBaseQuery(book)
                                .where(book.name().eq("Learning GraphQL"))
                                .where(book.edition().eq(3))
                                .addSelect(book)
                                .addSelect(
                                        getSqlClient().createSubQuery(authorEx)
                                                .where(authorEx.books().id().eq(book.id()))
                                                .select(Expression.rowCount())
                                ),
                        getSqlClient()
                                .createBaseQuery(store)
                                .where(store.name().eq("MANNING"))
                                .where(store.asTableEx().books().edition().eq(3))
                                .addSelect(store.asTableEx().books())
                                .addSelect(
                                        getSqlClient().createSubQuery(authorEx)
                                                .where(authorEx.books().id().eq(store.asTableEx().books().id()))
                                                .select(Expression.rowCount())
                                )
                ).asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_2().gt(0L))
                        .select(baseTable.get_1()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, " +
                                    "--->--->tb_2_.NAME c2, " +
                                    "--->--->tb_2_.EDITION c3, " +
                                    "--->--->tb_2_.PRICE c4, " +
                                    "--->--->tb_2_.STORE_ID c5, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_3_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                                    "--->--->--->where tb_4_.BOOK_ID = tb_2_.ID" +
                                    "--->--->) c6 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->where tb_2_.NAME = ? and tb_2_.EDITION = ? " +
                                    "--->union all " +
                                    "--->select " +
                                    "--->--->tb_7_.ID c1, " +
                                    "--->--->tb_7_.NAME c2, " +
                                    "--->--->tb_7_.EDITION c3, " +
                                    "--->--->tb_7_.PRICE c4, " +
                                    "--->--->tb_7_.STORE_ID c5, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_8_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_9_ on tb_8_.ID = tb_9_.AUTHOR_ID " +
                                    "--->--->--->where tb_9_.BOOK_ID = tb_7_.ID" +
                                    "--->--->) c6 " +
                                    "--->from BOOK_STORE tb_6_ " +
                                    "--->inner join BOOK tb_7_ on tb_6_.ID = tb_7_.STORE_ID " +
                                    "--->where tb_6_.NAME = ? and tb_7_.EDITION = ?" +
                                    ") tb_1_ " +
                                    "where tb_1_.c6 > ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":51.00," +
                                    "--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "},{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":80.00," +
                                    "--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testMergedBaseQueryWithJoinFetch() {
        BookTable book = BookTable.$;
        BookStoreTable store = BookStoreTable.$;
        AuthorTableEx authorEx = AuthorTableEx.$;
        BaseTable2<BookTable, NumericExpression<Long>> baseTable =
                TypedBaseQuery.unionAll(
                        getSqlClient()
                                .createBaseQuery(book)
                                .where(book.name().eq("Learning GraphQL"))
                                .where(book.edition().eq(3))
                                .addSelect(book)
                                .addSelect(
                                        getSqlClient().createSubQuery(authorEx)
                                                .where(authorEx.books().id().eq(book.id()))
                                                .select(Expression.rowCount())
                                ),
                        getSqlClient()
                                .createBaseQuery(store)
                                .where(store.name().eq("MANNING"))
                                .where(store.asTableEx().books().edition().eq(3))
                                .addSelect(store.asTableEx().books())
                                .addSelect(
                                        getSqlClient().createSubQuery(authorEx)
                                                .where(authorEx.books().id().eq(store.asTableEx().books().id()))
                                                .select(Expression.rowCount())
                                )
                ).asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_2().gt(0L))
                        .select(
                                baseTable.get_1().fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .store(
                                                        ReferenceFetchType.JOIN_ALWAYS,
                                                        BookStoreFetcher.$.allScalarFields()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, " +
                                    "--->tb_1_.c5, tb_1_.c6, tb_1_.c7, tb_1_.c8 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, " +
                                    "--->--->tb_3_.ID c5, tb_3_.NAME c6, tb_3_.WEBSITE c7, tb_3_.VERSION c8, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_4_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where tb_5_.BOOK_ID = tb_2_.ID" +
                                    "--->--->) c9 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->left join BOOK_STORE tb_3_ on tb_2_.STORE_ID = tb_3_.ID " +
                                    "--->where tb_2_.NAME = ? and tb_2_.EDITION = ? " +
                                    "--->union all " +
                                    "--->select " +
                                    "--->--->tb_8_.ID c1, tb_8_.NAME c2, tb_8_.EDITION c3, tb_8_.PRICE c4, " +
                                    "--->--->tb_9_.ID c5, tb_9_.NAME c6, tb_9_.WEBSITE c7, tb_9_.VERSION c8, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_10_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_11_ on tb_10_.ID = tb_11_.AUTHOR_ID " +
                                    "--->--->--->where tb_11_.BOOK_ID = tb_8_.ID" +
                                    "--->--->) c9 " +
                                    "--->from BOOK_STORE tb_7_ " +
                                    "--->inner join BOOK tb_8_ on tb_7_.ID = tb_8_.STORE_ID " +
                                    "--->left join BOOK_STORE tb_9_ on tb_8_.STORE_ID = tb_9_.ID " +
                                    "--->where tb_7_.NAME = ? and tb_8_.EDITION = ?" +
                                    ") tb_1_ " +
                                    "where tb_1_.c9 > ?"
                    );
                    ctx.rows(
                            "[{" +
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
                                    "},{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"edition\":3," +
                                    "--->\"price\":80.00," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testMergedBaseQueryWithOutsideJoinFetchAndInsideJoin() {
        BookTable book = BookTable.$;
        BookStoreTable store = BookStoreTable.$;
        AuthorTableEx authorEx = AuthorTableEx.$;
        BaseTable2<BookTable, NumericExpression<Long>> baseTable =
                TypedBaseQuery.unionAll(
                        getSqlClient()
                                .createBaseQuery(book)
                                .where(book.name().eq("Learning GraphQL"))
                                .where(book.edition().eq(3))
                                .addSelect(book)
                                .addSelect(
                                        getSqlClient().createSubQuery(authorEx)
                                                .where(authorEx.books().id().eq(book.id()))
                                                .select(Expression.rowCount())
                                ),
                        getSqlClient()
                                .createBaseQuery(store)
                                .where(store.name().eq("MANNING"))
                                .where(store.asTableEx().books().edition().eq(3))
                                .where(store.asTableEx().books().authors().gender().eq(Gender.MALE))
                                .addSelect(store.asTableEx().books())
                                .addSelect(
                                        getSqlClient().createSubQuery(authorEx)
                                                .where(authorEx.books().id().eq(store.asTableEx().books().id()))
                                                .select(Expression.rowCount())
                                )
                ).asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_2().gt(0L))
                        .where(baseTable.get_1().edition().between(1, 3))
                        .select(
                                baseTable.get_1().fetch(
                                        BookFetcher.$
                                                .name()
                                                .store(
                                                        ReferenceFetchType.JOIN_ALWAYS,
                                                        BookStoreFetcher.$.name()
                                                )
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_3_.ID c3, tb_3_.NAME c4, " +
                                    "--->--->tb_2_.EDITION c6, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_4_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_5_ " +
                                    "--->--->--->on tb_4_.ID = tb_5_.AUTHOR_ID " +
                                    "--->--->--->where tb_5_.BOOK_ID = tb_2_.ID" +
                                    "--->--->) c5 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->left join BOOK_STORE tb_3_ on tb_2_.STORE_ID = tb_3_.ID " +
                                    "--->where tb_2_.NAME = ? and tb_2_.EDITION = ? " +
                                    "--->union all " +
                                    "--->select " +
                                    "--->--->tb_8_.ID c1, tb_8_.NAME c2, tb_11_.ID c3, tb_11_.NAME c4, " +
                                    "--->--->tb_8_.EDITION c6, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from AUTHOR tb_12_ " +
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_13_ on tb_12_.ID = tb_13_.AUTHOR_ID " +
                                    "--->--->--->where tb_13_.BOOK_ID = tb_8_.ID" +
                                    "--->--->) c5 " +
                                    "--->from BOOK_STORE tb_7_ " +
                                    "--->inner join BOOK tb_8_ on tb_7_.ID = tb_8_.STORE_ID " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_9_ on tb_8_.ID = tb_9_.BOOK_ID " +
                                    "--->inner join AUTHOR tb_10_ on tb_9_.AUTHOR_ID = tb_10_.ID " +
                                    "--->left join BOOK_STORE tb_11_ on tb_8_.STORE_ID = tb_11_.ID " +
                                    "--->where tb_7_.NAME = ? and tb_8_.EDITION = ? and tb_10_.GENDER = ?) " +
                                    "tb_1_ " +
                                    "where tb_1_.c5 > ? and (tb_1_.c6 between ? and ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->\"name\":\"Learning GraphQL\"," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"" +
                                    "--->}" +
                                    "},{" +
                                    "--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->\"name\":\"GraphQL in Action\"," +
                                    "--->\"store\":{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"" +
                                    "--->}" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testSqlFormula() {
        AuthorTable author = AuthorTable.$;
        BaseTable1<AuthorTable> baseTable = TypedBaseQuery.unionAll(
                getSqlClient().createBaseQuery(author)
                        .where(author.id().eq(Constants.alexId))
                        .addSelect(author),
                getSqlClient().createBaseQuery(author)
                        .where(author.id().eq(Constants.borisId))
                        .addSelect(author)
        ).asBaseTable();
        executeAndExpect(
                getSqlClient().createQuery(baseTable)
                        .select(
                                baseTable.get_1().fetch(
                                        AuthorFetcher.$
                                                .fullName2()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.c1, tb_1_.c2 from (" +
                                    "--->select tb_2_.ID c1, concat(tb_2_.FIRST_NAME, ' ', tb_2_.LAST_NAME) c2 " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->where tb_2_.ID = ? " +
                                    "--->union all " +
                                    "--->select tb_3_.ID c1, concat(tb_3_.FIRST_NAME, ' ', tb_3_.LAST_NAME) c2 " +
                                    "--->from AUTHOR tb_3_ " +
                                    "--->where tb_3_.ID = ?" +
                                    ") tb_1_"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->\"fullName2\":\"Alex Banks\"" +
                                    "},{" +
                                    "--->\"id\":\"718795ad-77c1-4fcf-994a-fec6a5a11f0f\"," +
                                    "--->\"fullName2\":\"Boris Cherny\"" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testFetchDefaultEmbeddable() {
        TransformTable transform = TransformTable.$;
        BaseTable1<TransformTable> baseTable = TypedBaseQuery.unionAll(
                getSqlClient()
                        .createBaseQuery(transform)
                        .where(transform.id().eq(1L))
                        .addSelect(transform),
                getSqlClient()
                        .createBaseQuery(transform)
                        .where(transform.id().eq(2L))
                        .addSelect(transform)
        ).asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .select(
                                baseTable.get_1().fetch(
                                        TransformFetcher.$
                                                .source()
                                                .target()
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, " +
                                    "--->tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, " +
                                    "--->tb_1_.c6, tb_1_.c7, tb_1_.c8, tb_1_.c9 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, " +
                                    "--->--->tb_2_.`LEFT` c2, tb_2_.TOP c3, tb_2_.`RIGHT` c4, tb_2_.BOTTOM c5, " +
                                    "--->--->tb_2_.TARGET_LEFT c6, tb_2_.TARGET_TOP c7, tb_2_.TARGET_RIGHT c8, tb_2_.TARGET_BOTTOM c9 " +
                                    "--->from TRANSFORM tb_2_ " +
                                    "--->where tb_2_.ID = ? " +
                                    "--->union all " +
                                    "--->select " +
                                    "--->--->tb_3_.ID c1, " +
                                    "--->--->tb_3_.`LEFT` c2, tb_3_.TOP c3, tb_3_.`RIGHT` c4, tb_3_.BOTTOM c5, " +
                                    "--->--->tb_3_.TARGET_LEFT c6, tb_3_.TARGET_TOP c7, tb_3_.TARGET_RIGHT c8, tb_3_.TARGET_BOTTOM c9 " +
                                    "--->from TRANSFORM tb_3_ " +
                                    "--->where tb_3_.ID = ?" +
                                    ") tb_1_"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":1," +
                                    "--->\"source\":{\"leftTop\":{\"x\":100,\"y\":120},\"rightBottom\":{\"x\":400,\"y\":320}}," +
                                    "--->\"target\":{\"leftTop\":{\"x\":800,\"y\":600},\"rightBottom\":{\"x\":1400,\"y\":1000}}" +
                                    "},{" +
                                    "--->\"id\":2," +
                                    "--->\"source\":{\"leftTop\":{\"x\":150,\"y\":170},\"rightBottom\":{\"x\":450,\"y\":370}}," +
                                    "--->\"target\":null" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testFetchShapedEmbeddable() {
        TransformTable transform = TransformTable.$;
        BaseTable1<TransformTable> baseTable = TypedBaseQuery.unionAll(
                getSqlClient()
                        .createBaseQuery(transform)
                        .where(transform.id().eq(1L))
                        .addSelect(transform),
                getSqlClient()
                        .createBaseQuery(transform)
                        .where(transform.id().eq(2L))
                        .addSelect(transform)
        ).asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .select(
                                baseTable.get_1().fetch(
                                        TransformFetcher.$
                                                .source(RectFetcher.$.leftTop())
                                                .target(RectFetcher.$.rightBottom())
                                )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, " +
                                    "--->tb_1_.c2, tb_1_.c3, " +
                                    "--->tb_1_.c4, tb_1_.c5 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, " +
                                    "--->--->tb_2_.`LEFT` c2, tb_2_.TOP c3, " +
                                    "--->--->tb_2_.TARGET_RIGHT c4, tb_2_.TARGET_BOTTOM c5 " +
                                    "--->from TRANSFORM tb_2_ " +
                                    "--->where tb_2_.ID = ? " +
                                    "--->union all " +
                                    "--->select " +
                                    "--->--->tb_3_.ID c1, " +
                                    "--->--->tb_3_.`LEFT` c2, tb_3_.TOP c3, " +
                                    "--->--->tb_3_.TARGET_RIGHT c4, tb_3_.TARGET_BOTTOM c5 " +
                                    "--->from TRANSFORM tb_3_ " +
                                    "--->where tb_3_.ID = ?" +
                                    ") tb_1_"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"id\":1," +
                                    "--->\"source\":{\"leftTop\":{\"x\":100,\"y\":120}}," +
                                    "--->\"target\":{\"rightBottom\":{\"x\":1400,\"y\":1000}}" +
                                    "},{" +
                                    "--->\"id\":2," +
                                    "--->\"source\":{\"leftTop\":{\"x\":150,\"y\":170}}," +
                                    "--->\"target\":null" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testWeakJoinBaseTable() {
        BookTable book = BookTable.$;
        BaseTable1<BookTable> baseBook = TypedBaseQuery.unionAll(
                getSqlClient().createBaseQuery(book)
                        .where(book.id().eq(Constants.graphQLInActionId1))
                        .addSelect(book),
                getSqlClient().createBaseQuery(book)
                        .where(book.id().eq(Constants.graphQLInActionId2))
                        .addSelect(book)
        ).asBaseTable();
        AuthorTable author = AuthorTable.$;
        BaseTable1<AuthorTable> baseAuthor = TypedBaseQuery.unionAll(
                getSqlClient().createBaseQuery(author)
                        .where(author.id().eq(Constants.danId))
                        .addSelect(author),
                getSqlClient().createBaseQuery(author)
                        .where(author.id().eq(Constants.alexId))
                        .addSelect(author)
        ).asBaseTable();
        executeAndExpect(
                getSqlClient().createQuery(baseBook)
                        .select(
                                baseBook.get_1(),
                                baseBook.weakJoin(baseAuthor, BaseBookAuthorJoin.class).get_1()
                        ),
                ctx -> {
                    ctx.sql(
                            ""
                    );
                }
        );
    }

    private static class BaseBookAuthorJoin implements WeakJoin<BaseTable1<BookTable>, BaseTable1<AuthorTable>> {

        @Override
        public Predicate on(BaseTable1<BookTable> source, BaseTable1<AuthorTable> target) {
            return source.get_1().asTableEx().authors().eq(target.get_1());
        }
    }
}
