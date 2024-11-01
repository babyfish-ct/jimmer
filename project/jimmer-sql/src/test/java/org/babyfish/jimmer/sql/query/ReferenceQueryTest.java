package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.BookFetcher;
import org.babyfish.jimmer.sql.model.BookProps;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.hr.EmployeeFetcher;
import org.babyfish.jimmer.sql.model.hr.EmployeeProps;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReferenceQueryTest extends AbstractQueryTest {

    @Test
    public void testWithIdView() {
        BookTable table = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().eq("GraphQL in Action"))
                        .where(table.edition().eq(1))
                        .select(
                                table.fetch(BookFetcher.$.allReferenceFields())
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.STORE_ID " +
                                    "from BOOK tb_1_ " +
                                    "where tb_1_.NAME = ? and tb_1_.EDITION = ?"
                    );
                    ctx.row(0, book -> {
                        assertContentEquals(
                                "{" +
                                        "--->\"id\":\"a62f7aa3-9490-4612-98b5-98aae0e77120\"," +
                                        "--->\"storeId\":\"2fa3955e-3e83-49b9-902e-0465c109c779\"" +
                                        "}",
                                book
                        );
                        Assertions.assertTrue(ImmutableObjects.isLoaded(book, BookProps.STORE_ID));
                        Assertions.assertTrue(ImmutableObjects.isLoaded(book, BookProps.STORE));
                    });
                }
        );
    }

    @Test
    public void testWithoutIdView() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(
                                table.fetch(EmployeeFetcher.$.allReferenceFields())
                        ),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.DEPARTMENT_ID " +
                                    "from EMPLOYEE tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?"
                    );
                    ctx.row(0, employee -> {
                        assertContentEquals(
                                "{" +
                                        "--->\"id\":\"1\"," +
                                        "--->\"department\":{\"id\":\"1\"}" +
                                        "}",
                                employee
                        );
                        Assertions.assertTrue(
                                ImmutableObjects.isLoaded(employee, EmployeeProps.DEPARTMENT)
                        );
                    });
                }
        );
    }
}
