package org.babyfish.jimmer.sql.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.common.Constants;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.OrderIdDraft;
import org.babyfish.jimmer.sql.model.embedded.OrderItemTable;
import org.babyfish.jimmer.sql.model.embedded.OrderTable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class IssueTest extends AbstractQueryTest {

    @Test
    public void testIssue1238() {
        BookTable table = BookTable.$;
        BaseTable1<BookTable> baseTable = getSqlClient()
                .createBaseQuery(table)
                .where(
                        table.authors(a -> a.firstName().eq("Alex"))
                )
                .addSelect(table)
                .asBaseTable();
        executeAndExpect(
                getSqlClient().createQuery(baseTable)
                        .select(baseTable.get_1()),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, tb_2_.STORE_ID c5 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->where exists(" +
                                    "--->--->select 1 from AUTHOR tb_3_ " +
                                    "--->--->inner join BOOK_AUTHOR_MAPPING tb_4_ on tb_3_.ID = tb_4_.AUTHOR_ID " +
                                    "--->--->where tb_4_.BOOK_ID = tb_2_.ID and tb_3_.FIRST_NAME = ?" +
                                    "--->)" +
                                    ") tb_1_"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"id\":\"e110c564-23cc-4811-9e81-d587a13db634\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":1," +
                                    "--->--->\"price\":50.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"b649b11b-1161-4ad2-b261-af0112fdd7c8\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":2," +
                                    "--->--->\"price\":55.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}," +
                                    "--->{" +
                                    "--->--->\"id\":\"64873631-5d82-4bae-8eb8-72dd955bfc56\"," +
                                    "--->--->\"name\":\"Learning GraphQL\"," +
                                    "--->--->\"edition\":3," +
                                    "--->--->\"price\":51.00," +
                                    "--->--->\"storeId\":\"d38c10da-6be8-4924-b9b9-5e81899612a0\"" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testIssue1244() {
        Assumptions.abort("It is hard to resolve this bug");
        TreeNodeTable table = TreeNodeTable.$;
        BaseTable1<TreeNodeTable> baseTable = TypedBaseQuery.unionAllRecursively(
                getSqlClient()
                        .createBaseQuery(table)
                        .where(table.id().in(Collections.singletonList(1L)))
                        .addSelect(table),
                recursiveRef -> getSqlClient()
                        .createBaseQuery(table, recursiveRef, (source, target) -> {
                            return source.parentId().eq(target.get_1().id());
                        })
                        .addSelect(table)
        ).asCteBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_1().parent().name().ilike("e"))
                        .select(baseTable.get_1()),
                ctx -> {

                }
        );
    }

    @Test
    public void testNewIssue() {
        Assumptions.abort("It is hard to resolve this bug");
        TreeNodeTable table = TreeNodeTable.$;
        BaseTable1<TreeNodeTable> baseTable = getSqlClient()
                .createBaseQuery(table)
                .where(table.parentId().isNull())
                .addSelect(table)
                .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_1().parent().name().like("shirt"))
                        .select(baseTable.get_1()),
                ctx -> {
                    ctx.sql(
                            ""
                    );
                }
        );
    }

    @Test
    public void testIssue1258() {
        Assumptions.abort("It is hard to resolve this bug");
        BookTable table = BookTable.$;
        BaseTable1<BookTable> baseTable =
                getSqlClient()
                        .createBaseQuery(table)
                        .addSelect(table)
                        .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_1().storeId().eq(Constants.manningId))
                        .select(baseTable.get_1(), baseTable.get_1().store()),
                ctx -> {

                }
        );
    }

    @Test
    public void testIssueLike1258() {
        BookTable table = BookTable.$;
        BaseTable2<BookTable, BookStoreTable> baseTable =
                getSqlClient()
                        .createBaseQuery(table)
                        .addSelect(table)
                        .addSelect(table.store())
                        .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_2().id().eq(Constants.manningId))
                        .select(baseTable.get_1(), baseTable.get_2()),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, " +
                                    "--->tb_1_.c6, tb_1_.c7, tb_1_.c8, tb_1_.c9 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, tb_2_.EDITION c3, tb_2_.PRICE c4, tb_2_.STORE_ID c5, " +
                                    "--->--->tb_2_.STORE_ID c6, tb_3_.NAME c7, tb_3_.WEBSITE c8, tb_3_.VERSION c9 " +
                                    "--->from BOOK tb_2_ " +
                                    "--->inner join BOOK_STORE tb_3_ on tb_2_.STORE_ID = tb_3_.ID" +
                                    ") tb_1_ where tb_1_.c6 = ?"
                    );
                    ctx.rows(
                            "[" +
                                    "--->{" +
                                    "--->--->\"_1\":{" +
                                    "--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":1," +
                                    "--->--->--->\"price\":80.00," +
                                    "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}," +
                                    "--->--->\"_2\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"_1\":{" +
                                    "--->--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":2," +
                                    "--->--->--->\"price\":81.00," +
                                    "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->}," +
                                    "--->--->\"_2\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}" +
                                    "--->},{" +
                                    "--->--->\"_1\":{" +
                                    "--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->\"name\":\"GraphQL in Action\"," +
                                    "--->--->--->\"edition\":3," +
                                    "--->--->--->\"price\":80.00," +
                                    "--->--->--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                    "--->--->},\"_2\":{" +
                                    "--->--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->--->\"name\":\"MANNING\"," +
                                    "--->--->--->\"website\":null," +
                                    "--->--->--->\"version\":0" +
                                    "--->--->}" +
                                    "--->}" +
                                    "]"
                    );
                }
        );
    }

    @Test
    public void testIssueLike1258ByMultiColumnForeignKey() {
        OrderItemTable table = OrderItemTable.$;
        BaseTable2<OrderItemTable, OrderTable> baseTable =
                getSqlClient()
                        .createBaseQuery(table)
                        .addSelect(table)
                        .addSelect(table.order())
                        .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(
                                baseTable.get_2().id().eq(
                                        OrderIdDraft.$.produce(id -> id.setX("001").setY("001"))
                                )
                        )
                        .select(baseTable.get_1(), baseTable.get_2()),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4, tb_1_.c5, tb_1_.c6, " +
                                    "--->tb_1_.c7, tb_1_.c8, tb_1_.c9 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ORDER_ITEM_A c1, tb_2_.ORDER_ITEM_B c2, tb_2_.ORDER_ITEM_C c3, tb_2_.NAME c4, tb_2_.FK_ORDER_X c5, tb_2_.FK_ORDER_Y c6, " +
                                    "--->--->tb_2_.FK_ORDER_X c7, tb_2_.FK_ORDER_Y c8, tb_3_.NAME c9 " +
                                    "--->from ORDER_ITEM tb_2_ " +
                                    "--->inner join ORDER_ tb_3_ on tb_2_.FK_ORDER_X = tb_3_.ORDER_X and tb_2_.FK_ORDER_Y = tb_3_.ORDER_Y" +
                                    ") tb_1_ where (tb_1_.c7, tb_1_.c8) = (?, ?)"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"_1\":{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":1}," +
                                    "--->--->\"name\":\"order-item-1-1\"," +
                                    "--->--->\"order\":{\"id\":{\"x\":\"001\",\"y\":\"001\"}}" +
                                    "--->},\"_2\":{" +
                                    "--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->\"name\":\"order-1\"" +
                                    "--->}},{" +
                                    "--->\"_1\":{" +
                                    "--->--->\"id\":{\"a\":1,\"b\":1,\"c\":2}," +
                                    "--->--->\"name\":\"order-item-1-2\"," +
                                    "--->--->\"order\":{\"id\":{\"x\":\"001\",\"y\":\"001\"}}" +
                                    "--->},\"_2\":{" +
                                    "--->--->\"id\":{\"x\":\"001\",\"y\":\"001\"}," +
                                    "--->--->\"name\":\"order-1\"" +
                                    "--->}" +
                                    "}]"
                    );
                }
        );
    }

    // Cannot reproduce
    @Test
    public void testIssue1290() {
        BookStoreTable store = BookStoreTable.$;
        BookTable book = BookTable.$;
        NumericExpression<Long> bookCount = getSqlClient()
                .createSubQuery(book)
                .where(book.store().eq(store))
                .selectCount();
        BaseTable3<BookStoreTable, NumericExpression<Long>, NumericExpression<Long>> baseTable = getSqlClient()
                .createBaseQuery(store)
                .where(bookCount.gt(0L))
                .addSelect(store)
                .addSelect(bookCount)
                .addSelect(
                        Expression.numeric().sql(
                                Long.class,
                                "row_number() over(order by %e desc)",
                                bookCount
                        )
                )
                .asBaseTable();
        executeAndExpect(
                getSqlClient()
                        .createQuery(baseTable)
                        .where(baseTable.get_1().id().eq(Constants.manningId))
                        .select(
                                baseTable.get_1().fetch(
                                        BookStoreFetcher.$
                                                .name()
                                                .books(
                                                        BookFetcher.$.
                                                                name()
                                                )
                                ),
                                baseTable.get_2(),
                                baseTable.get_3()
                        ),
                ctx -> {
                    ctx.sql(
                            "select " +
                                    "--->tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 " +
                                    "from (" +
                                    "--->select " +
                                    "--->--->tb_2_.ID c1, tb_2_.NAME c2, " +
                                    "--->--->(" +
                                    "--->--->--->select count(1) " +
                                    "--->--->--->from BOOK tb_3_ " +
                                    "--->--->--->where tb_3_.STORE_ID = tb_2_.ID" +
                                    "--->--->) c3, " +
                                    "--->--->row_number() over(" +
                                    "--->--->--->order by (" +
                                    "--->--->--->--->select count(1) " +
                                    "--->--->--->--->from BOOK tb_3_ " +
                                    "--->--->--->--->where tb_3_.STORE_ID = tb_2_.ID" +
                                    "--->--->--->) desc" +
                                    "--->--->) c4 " +
                                    "--->from BOOK_STORE tb_2_ " +
                                    "--->where (" +
                                    "--->--->select count(1) " +
                                    "--->--->from BOOK tb_3_ " +
                                    "--->--->where tb_3_.STORE_ID = tb_2_.ID" +
                                    "--->) > ?" +
                                    ") tb_1_ " +
                                    "where tb_1_.c1 = ?"
                    );
                    ctx.statement(1).sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.STORE_ID = ?"
                    );
                    ctx.rows(
                            "[{" +
                                    "--->\"_1\":{" +
                                    "--->--->\"id\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"," +
                                    "--->--->\"name\":\"MANNING\"," +
                                    "--->--->\"books\":[" +
                                    "--->--->--->{" +
                                    "--->--->--->--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"e37a8344-73bb-4b23-ba76-82eac11f03e6\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"" +
                                    "--->--->--->},{" +
                                    "--->--->--->--->\"id\":\"780bdf07-05af-48bf-9be9-f8c65236fecc\"," +
                                    "--->--->--->--->\"name\":\"GraphQL in Action\"" +
                                    "--->--->--->}" +
                                    "--->--->]" +
                                    "--->}," +
                                    "--->\"_2\":3," +
                                    "--->\"_3\":2" +
                                    "}]"
                    );
                }
        );
    }
}
