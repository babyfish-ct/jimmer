package org.babyfish.jimmer.sql.tuple;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookStoreFetcher;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.dto.BookViewForTupleTest;
import org.junit.jupiter.api.Test;

public class TypedTupleTest extends AbstractQueryTest {

    @Test
    public void testAggregateTupleAsBaseTable() {
        BookTable table = BookTable.$;
        AggregateTupleTable baseTable = getSqlClient()
                .createBaseQuery(table)
                .groupBy(table.storeId())
                .select(
                        AggregateTupleMapper
                                .storeId(table.storeId())
                                .bookCount(Expression.rowCount())
                                .minPrice(table.price().min())
                                .maxPrice(table.price().max())
                                .avgPrice(table.price().avgAsDecimal())
                )
                .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.getBookCount().gt(2L))
                        .orderBy(baseTable.getStoreId())
                        .select(baseTable.getStoreId(), baseTable.getAvgPrice()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.c1, tb_1_.c3 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.STORE_ID c1, " +
                                    "--->--->count(1) c2, " +
                                    "--->--->avg(tb_2_.PRICE) c3 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->group by tb_2_.STORE_ID" +
                                    ") tb_1_ " +
                                    "where tb_1_.c2 > ? " +
                                    "order by tb_1_.c1 asc"
                    );
                }
        );
    }

    @Test
    public void testAggregateTupleAsCteBaseTable() {
        BookTable table = BookTable.$;
        AggregateTupleTable baseTable = getSqlClient()
                .createBaseQuery(table)
                .groupBy(table.storeId())
                .select(
                        AggregateTupleMapper
                                .storeId(table.storeId())
                                .bookCount(Expression.rowCount())
                                .minPrice(table.price().min())
                                .maxPrice(table.price().max())
                                .avgPrice(table.price().avgAsDecimal())
                )
                .asCteBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.getBookCount().gt(2L))
                        .select(baseTable.getStoreId(), baseTable.getAvgPrice()),
                ctx -> {
                    ctx.sql(
                            "with tb_1_(c1, c2, c3) as (" +
                                    "--->select " +
                                    "--->--->tb_2_.STORE_ID, " +
                                    "--->--->count(1), " +
                                    "--->--->avg(tb_2_.PRICE) " +
                                    "--->from BOOK tb_2_ " +
                                    "--->group by tb_2_.STORE_ID" +
                                    ") " +
                                    "select tb_1_.c1, tb_1_.c3 " +
                                    "from tb_1_ " +
                                    "where tb_1_.c2 > ?"
                    );
                }
        );
    }

    @Test
    public void testEntityTupleAsBaseTable() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        EntityTupleTable baseTable = getSqlClient()
                .createBaseQuery(table)
                .where(table.edition().eq(3))
                .select(
                        EntityTupleMapper
                                .book(table)
                                .authorCount(
                                        getSqlClient()
                                                .createSubQuery(author)
                                                .where(author.books().id().eq(table.id()))
                                                .selectCount()
                                )
                )
                .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.getAuthorCount().gt(1L))
                        .select(baseTable.getBook()),
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
                                    "--->--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ " +
                                    "--->--->--->on tb_3_.ID = tb_4_.AUTHOR_ID " +
                                    "--->--->--->where tb_4_.BOOK_ID = tb_2_.ID" +
                                    "--->--->) c6 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->where tb_2_.EDITION = ?" +
                                    ") tb_1_ " +
                                    "where tb_1_.c6 > ?"
                    );
                }
        );
    }

    @Test
    public void testWideTupleAsBaseTable() {
        BookTable table = BookTable.$;
        WideTupleTable baseTable = getSqlClient()
                .createBaseQuery(table)
                .select(
                        WideTupleMapper
                                .value1(Expression.rowCount())
                                .value2(Expression.rowCount())
                                .value3(Expression.rowCount())
                                .value4(Expression.rowCount())
                                .value5(Expression.rowCount())
                                .value6(Expression.rowCount())
                                .value7(Expression.rowCount())
                                .value8(Expression.rowCount())
                                .value9(Expression.rowCount())
                                .value10(Expression.rowCount())
                )
                .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .select(baseTable.getValue10()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.c1 " +
                                    "from (" +
                                    "--->select count(1) c1 " +
                                    "--->from BOOK tb_2_" +
                                    ") tb_1_"
                    );
                }
        );
    }

    @Test
    public void testAggregateTuple() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .groupBy(table.storeId())
                        .select(
                                AggregateTupleMapper
                                        .storeId(table.storeId())
                                        .bookCount(Expression.rowCount())
                                        .minPrice(table.price().min())
                                        .maxPrice(table.price().max())
                                        .avgPrice(table.price().avgAsDecimal())
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.STORE_ID, " +
                                    "--->count(1), " +
                                    "--->min(tb_1_.PRICE), " +
                                    "--->max(tb_1_.PRICE), " +
                                    "--->avg(tb_1_.PRICE) " +
                                    "from BOOK tb_1_ " +
                                    "group by tb_1_.STORE_ID"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->\"bookCount\":3," +
                                    "--->\"minPrice\":80.00," +
                                    "--->\"maxPrice\":81.00," +
                                    "--->\"avgPrice\":80.333333333333" +
                                    "},{" +
                                    "--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->\"bookCount\":9," +
                                    "--->\"minPrice\":45.00," +
                                    "--->\"maxPrice\":88.00," +
                                    "--->\"avgPrice\":58.500000000000" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testRawTableTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                EntityTupleMapper
                                        .book(table)
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.ID, tb_1_.NAME, tb_1_.EDITION, tb_1_.PRICE, tb_1_.STORE_ID, " +
                                    "--->(" +
                                    "--->--->select count(1) " +
                                    "--->--->from AUTHOR tb_2_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    "--->) " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":88.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":48.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":80.00," +
                                    "--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testEntityTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                EntityTupleMapper
                                        .book(
                                                table.fetch(
                                                        BookFetcher.$.name()
                                                                .store(
                                                                        BookStoreFetcher.$.name()
                                                                )
                                                )
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testLombokEntityTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                LombokEntityTupleMapper
                                        .book(
                                                table.fetch(
                                                        BookFetcher.$.name()
                                                                .store(
                                                                        BookStoreFetcher.$.name()
                                                                )
                                                )
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testDtoTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                DtoTupleMapper
                                        .book(
                                                table.fetch(BookViewForTupleTest.class)
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }

    @Test
    public void testLombokDtoTuple() {
        BookTable table = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpect(
                getSqlClient().createQuery(table)
                        .where(table.edition().eq(3))
                        .select(
                                LombokDtoTupleMapper
                                        .book(
                                                table.fetch(BookViewForTupleTest.class)
                                        )
                                        .authorCount(
                                                getSqlClient().createSubQuery(author)
                                                        .where(author.books().id().eq(table.id()))
                                                        .selectCount()
                                        )
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "tb_1_.ID, tb_1_.NAME, tb_1_.STORE_ID, " +
                                    "(" +
                                    "--->select count(1) " +
                                    "--->from AUTHOR tb_2_ " +
                                    "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                    "--->where tb_3_.BOOK_ID = tb_1_.ID" +
                                    ") from BOOK tb_1_ " +
                                    "where tb_1_.EDITION = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK_STORE tb_1_ " +
                                    "where tb_1_.ID in (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":2" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"9eded40f-6d2e-41de-b4e7-33a28b11c8b6\"," +
                                    "--->--->\"name\":\"Effective TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"782b9a9d-eac8-41c4-9f2d-74a5d047f45a\"," +
                                    "--->--->\"name\":\"Programming TypeScript\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->--->\"name\":\"O'REILLY\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "},{" +
                                    "--->\"book\":{" +
                                    "--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->\"authorCount\":1" +
                                    "}]"
                    );
                }
        );
    }
}
