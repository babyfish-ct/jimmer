package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class SQLiteFluentDMLTest extends AbstractMutationTest {
    @BeforeAll
    public static void beforeAll() {
        jdbc(NativeDatabases.SQLITE_DATA_SOURCE, false, con -> initDatabase(con, "database-sqlite.sql"));
    }

    @Test
    public void testUpdate() {
        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createUpdate(book)
                        .set(book.price(), book.price().plus(BigDecimal.ONE))
                        .where(
                                book.id().in(
                                        getSqlClient()
                                                .createSubQuery(author)
                                                .where(author.firstName().eq("Alex"))
                                                .select(author.books().id())
                                )
                        ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK " +
                                        "set PRICE = BOOK.PRICE + ? " +
                                        "where BOOK.ID in (" +
                                        "--->select tb_3_.BOOK_ID " +
                                        "--->from AUTHOR tb_2_ " +
                                        "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "--->where tb_2_.FIRST_NAME = ?" +
                                        ")");
                    });
                    ctx.rowCount(3);
                }
        );
    }

    @Test
    public void testUpdateWithNullArgument() {
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createUpdate(store)
                        .set(store.website(), Expression.nullValue(String.class)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE set WEBSITE = null");
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testUpdateJoinBySQLite() {
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createUpdate(author)
                        .set(author.firstName(), author.firstName().concat("*"))
                        .where(author.books().store().name().eq("MANNING")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update AUTHOR " +
                                        "set FIRST_NAME = concat(AUTHOR.FIRST_NAME, ?) " +
                                        "from BOOK_AUTHOR_MAPPING tb_2_ " +
                                        "inner join BOOK tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                        "inner join BOOK_STORE tb_4_ on " +
                                        "tb_3_.STORE_ID = tb_4_.ID " +
                                        "where " +
                                        "AUTHOR.ID = tb_2_.AUTHOR_ID " +
                                        "and " +
                                        "tb_4_.NAME = ?"
                        );
                        it.variables("*", "MANNING");
                    });
                }
        );
    }

    @Test
    public void testUpdateJoinBySQLiteErrorByOuterJoin() {
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createUpdate(author)
                        .set(author.firstName(), author.firstName().concat("*"))
                        .where(author.books(JoinType.LEFT).store().name().eq("MANNING")),
                ctx -> {
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                        it.message(
                                "The first level table joins cannot be outer join because current dialect " +
                                        "'org.babyfish.jimmer.sql.dialect.SQLiteDialect' " +
                                        "indicates that the first level table joins in update statement must be " +
                                        "rendered as 'from' clause, but there is a first level table join whose " +
                                        "join type is outer: 'Author.books(left)'."
                        );
                    });
                }
        );
    }

    @Test
    public void testUpdateWithFilter() {
        BookTable table = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createUpdate(table)
                        .set(table.name(), table.name().concat("*"))
                        .where(table.name().ilike("Learning GraphQL")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update BOOK " +
                                        "set NAME = concat(BOOK.NAME, ?) " +
                                        "where lower(BOOK.NAME) like ?"
                        );
                    });
                }
        );
    }

    @Test
    public void testDelete() {
        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createDelete(book)
                        .where(
                                book.id().in(
                                        getSqlClient()
                                                .createSubQuery(author)
                                                .where(author.firstName().eq("Alex"))
                                                .select(author.books().id())
                                )
                        )
                        .disableDissociation(),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK " +
                                        "where BOOK.ID in (" +
                                        "--->select tb_3_.BOOK_ID " +
                                        "--->from AUTHOR tb_2_ " +
                                        "--->inner join BOOK_AUTHOR_MAPPING tb_3_ on tb_2_.ID = tb_3_.AUTHOR_ID " +
                                        "--->where tb_2_.FIRST_NAME = ?" +
                                        ")"
                        );
                    });
                    ctx.rowCount(3);
                }
        );
    }

    @Test
    public void testPhysicallyDeleteByAssociatedId() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createDelete(table)
                        .setMode(DeleteMode.PHYSICAL)
                        .where(table.departmentId().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from EMPLOYEE " +
                                        "where EMPLOYEE.DEPARTMENT_ID = ? " +
                                        "and EMPLOYEE.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testLogicallyDeleteByAssociatedId() {
        EmployeeTable table = EmployeeTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createDelete(table)
                        .where(table.departmentId().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE " +
                                        "set DELETED_MILLIS = ? " +
                                        "where EMPLOYEE.DEPARTMENT_ID = ? " +
                                        "and EMPLOYEE.DELETED_MILLIS = ?"
                        );
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testDeleteWithJoin() {
        BookTable book = BookTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new SQLiteDialect())
                )
                        .createDelete(book)
                        .where(book.store().name().eq("MANNING")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select distinct BOOK.ID " +
                                        "from BOOK BOOK " +
                                        "inner join BOOK_STORE tb_2_ on BOOK.STORE_ID = tb_2_.ID " +
                                        "where tb_2_.NAME = ?"
                        );
                        it.variables("MANNING");
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK_AUTHOR_MAPPING where BOOK_ID in (?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where ID in (?, ?, ?)");
                        it.unorderedVariables(graphQLInActionId1, graphQLInActionId2, graphQLInActionId3);
                    });
                    ctx.rowCount(6);
                }
        );
    }
}
