package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.AuthorFetcher;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.embedded.OrderItemTable;
import org.junit.jupiter.api.Test;

public class MinOffsetTest extends AbstractQueryTest {

    @Test
    public void testBySelfWithIdOnly() {

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(book)
                        .where(book.store().name().eq("O'REILLY"))
                        .orderBy(book.name().asc(), book.edition().desc())
                        .select(
                                book.fetch(
                                        BookFetcher.$
                                )
                        )
                        .limit(3, 3),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from BOOK as tb_1_ " +
                                    "inner join BOOK_STORE as tb_2_ " +
                                    "--->on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_2_.NAME = ? " +
                                    "order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "limit ? " +
                                    "offset ?"
                    );
                    ctx.rows(
                            "[" +
                                    "{\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"}," +
                                    "{\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"}," +
                                    "{\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testBySelf() {

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(book)
                        .where(book.store().name().eq("O'REILLY"))
                        .orderBy(book.name().asc(), book.edition().desc())
                        .select(book)
                        .limit(3, 3),
                ctx -> {
                    ctx.sql(
                            "select optimize_.ID, optimize_.NAME, optimize_.EDITION, optimize_.PRICE, optimize_.STORE_ID " +
                                    "from (" +
                                    "--->select tb_1_.ID as optimize_core_id_ " +
                                    "--->from BOOK as tb_1_ " +
                                    "--->inner join BOOK_STORE as tb_2_ " +
                                    "--->--->on tb_1_.STORE_ID = tb_2_.ID " +
                                    "--->where tb_2_.NAME = ? " +
                                    "--->order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "--->limit ? offset ?" +
                                    ") optimize_core_ " +
                                    "inner join BOOK as optimize_ " +
                                    "--->on optimize_.ID = optimize_core_.optimize_core_id_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"store\":{" +
                                    "--->--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testBySelfWithAssociation() {

        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(book)
                        .where(book.store().name().eq("O'REILLY"))
                        .orderBy(book.name().asc(), book.edition().desc())
                        .select(
                                book.fetch(
                                        BookFetcher.$
                                                .allScalarFields()
                                                .price(false)
                                                .authors(
                                                        AuthorFetcher.$
                                                                .allScalarFields()
                                                )
                                )
                        )
                        .limit(3, 3),
                ctx -> {
                    ctx.sql(
                            "select optimize_.ID, optimize_.NAME, optimize_.EDITION from (" +
                                    "--->select tb_1_.ID as optimize_core_id_ " +
                                    "--->from BOOK as tb_1_ " +
                                    "--->inner join BOOK_STORE as tb_2_ " +
                                    "--->--->on tb_1_.STORE_ID = tb_2_.ID " +
                                    "--->where tb_2_.NAME = ? " +
                                    "--->order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "--->limit ? offset ?" +
                                    ") optimize_core_ " +
                                    "inner join BOOK as optimize_ " +
                                    "--->on optimize_.ID = optimize_core_.optimize_core_id_"
                    );
                    ctx.statement(1).sql(
                            "select tb_2_.BOOK_ID, tb_1_.ID, tb_1_.FIRST_NAME, tb_1_.LAST_NAME, tb_1_.GENDER " +
                                    "from AUTHOR as tb_1_ " +
                                    "inner join BOOK_AUTHOR_MAPPING as tb_2_ " +
                                    "--->on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                    "where tb_2_.BOOK_ID in (?, ?, ?)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"authors\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"1e93da94-af84-44f4-82d1-d8a9fd52ea94\"," +
                                    "--->--->--->--->\"firstName\":\"Alex\"," +
                                    "--->--->--->--->\"lastName\":\"Banks\"," +
                                    "--->--->--->--->\"gender\":\"MALE\"" +
                                    "--->--->--->}," +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"fd6bb6cf-336d-416c-8005-1ae11a6694b5\"," +
                                    "--->--->--->--->\"firstName\":\"Eve\"," +
                                    "--->--->--->--->\"lastName\":\"Procello\"," +
                                    "--->--->--->--->\"gender\":\"FEMALE\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testByReferenceJoin() {
        BookTable book = BookTable.$;

        executeAndExpect(
                getSqlClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(book)
                        .orderBy(book.name().asc(), book.edition().desc())
                        .select(book.store())
                        .limit(2, 2),
                ctx -> {
                    ctx.sql(
                            "select optimize_.ID, optimize_.NAME, optimize_.WEBSITE, optimize_.VERSION " +
                                    "from (" +
                                    "--->select tb_1_.STORE_ID as optimize_core_id_ " +
                                    "--->from BOOK as tb_1_ " +
                                    "--->order by tb_1_.NAME asc, tb_1_.EDITION desc " +
                                    "--->limit ? offset ?" +
                                    ") optimize_core_ " +
                                    "inner join BOOK_STORE as optimize_ " +
                                    "--->on optimize_.ID = optimize_core_.optimize_core_id_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"," +
                                    "--->--->\"name\":\"O'REILLY\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"website\":null," +
                                    "--->--->\"version\":0" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testByEmbeddedId() {
        OrderItemTable table = OrderItemTable.$;
        executeAndExpect(
                getSqlClient(cfg -> cfg.setMinOffsetForIdOnlyScanMode(1))
                        .createQuery(table)
                        .orderBy(table.name())
                        .select(table)
                        .limit(2, 2),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->optimize_.ORDER_ITEM_A, optimize_.ORDER_ITEM_B, optimize_.ORDER_ITEM_C, " +
                                    "--->optimize_.NAME, " +
                                    "--->optimize_.FK_ORDER_X, " +
                                    "--->optimize_.FK_ORDER_Y " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_1_.ORDER_ITEM_A as optimize_core_id_, " +
                                    "--->--->tb_1_.ORDER_ITEM_B as optimize_core_id_1_, " +
                                    "--->--->tb_1_.ORDER_ITEM_C as optimize_core_id_2_ " +
                                    "--->from ORDER_ITEM as tb_1_ " +
                                    "--->order by tb_1_.NAME asc " +
                                    "--->limit ? offset ?" +
                                    ") optimize_core_ " +
                                    "inner join ORDER_ITEM as optimize_ " +
                                    "--->on " +
                                    "--->--->(" +
                                    "--->--->--->optimize_.ORDER_ITEM_A, optimize_.ORDER_ITEM_B, optimize_.ORDER_ITEM_C" +
                                    "--->--->) = (" +
                                    "--->--->--->optimize_core_.optimize_core_id_, optimize_core_.optimize_core_id_1_, optimize_core_.optimize_core_id_2_" +
                                    "--->--->)"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":{" +
                                    "--->--->--->\"a\":1," +
                                    "--->--->--->\"b\":2," +
                                    "--->--->--->\"c\":1" +
                                    "--->--->}," +
                                    "--->--->\"name\":\"order-item-2-1\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":{" +
                                    "--->--->--->--->\"x\":\"001\"," +
                                    "--->--->--->--->\"y\":\"002\"" +
                                    "--->--->--->}" +
                                    "--->--->}" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":{" +
                                    "--->--->--->\"a\":2," +
                                    "--->--->--->\"b\":1," +
                                    "--->--->--->\"c\":1" +
                                    "--->--->}," +
                                    "--->--->\"name\":\"order-item-2-2\"," +
                                    "--->--->\"order\":{" +
                                    "--->--->--->\"id\":{" +
                                    "--->--->--->--->\"x\":\"001\"," +
                                    "--->--->--->--->\"y\":\"002\"" +
                                    "--->--->--->}" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }
}
