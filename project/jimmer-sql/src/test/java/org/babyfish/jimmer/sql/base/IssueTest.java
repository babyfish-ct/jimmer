package org.babyfish.jimmer.sql.base;

import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.TreeNodeTable;
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
}
