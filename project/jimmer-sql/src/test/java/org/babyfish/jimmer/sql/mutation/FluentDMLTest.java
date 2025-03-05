package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.hr.EmployeeTable;
import org.babyfish.jimmer.sql.model.inheritance.AdministratorTable;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.babyfish.jimmer.sql.common.Constants.graphQLInActionId3;

public class FluentDMLTest extends AbstractMutationTest {

    @Test
    public void testUpdate() {
        AdministratorTable t = AdministratorTable.$;
        getSqlClient().createUpdate(t)
                .set(t.createdTime(), LocalDateTime.now())
                .where(t.id().eq(1L));
        BookTable book = BookTable.$;
        AuthorTableEx author = AuthorTableEx.$;
        executeAndExpectRowCount(
                getSqlClient()
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
                                "update BOOK tb_1_ " +
                                        "set PRICE = tb_1_.PRICE + ? " +
                                        "where tb_1_.ID in (" +
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
                getSqlClient()
                        .createUpdate(store)
                        .set(store.website(), Expression.nullValue(String.class)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE tb_1_ set WEBSITE = null");
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testUpdateJoinByMySql() {

        NativeDatabases.assumeNativeDatabase();

        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpectRowCount(
                NativeDatabases.MYSQL_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new MySqlDialect())
                ).createUpdate(author)
                        .set(author.firstName(), author.firstName().concat("*"))
                        .set(author.books().name(), author.books().name().concat("*"))
                        .set(author.books().store().name(), author.books().store().name().concat("*"))
                        .where(author.books().store().name().eq("MANNING")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update AUTHOR tb_1_ " +
                                        "inner join BOOK_AUTHOR_MAPPING tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                        "inner join BOOK tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                        "inner join BOOK_STORE tb_4_ on tb_3_.STORE_ID = tb_4_.ID " +
                                        "set " +
                                        "tb_1_.FIRST_NAME = concat(tb_1_.FIRST_NAME, ?), " +
                                        "tb_3_.NAME = concat(tb_3_.NAME, ?), " +
                                        "tb_4_.NAME = concat(tb_4_.NAME, ?) " +
                                        "where tb_4_.NAME = ?"
                        );
                        it.variables("*", "*", "*", "MANNING");
                    });
                }
        );
    }

    @Test
    public void testUpdateJoinByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpectRowCount(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new PostgresDialect())
                ).createUpdate(author)
                        .set(author.firstName(), author.firstName().concat("*"))
                        .where(author.books().store().name().eq("MANNING")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update AUTHOR tb_1_ " +
                                        "set FIRST_NAME = concat(tb_1_.FIRST_NAME, ?) " +
                                        "from BOOK_AUTHOR_MAPPING tb_2_ " +
                                        "inner join BOOK tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                        "inner join BOOK_STORE tb_4_ on " +
                                        "tb_3_.STORE_ID = tb_4_.ID " +
                                        "where " +
                                        "tb_1_.ID = tb_2_.AUTHOR_ID " +
                                        "and " +
                                        "tb_4_.NAME = ?"
                        );
                        it.variables("*", "MANNING");
                    });
                }
        );
    }

    @Test
    public void testUpdateJoinByPostgresErrorByOuterJoin() {

        NativeDatabases.assumeNativeDatabase();

        AuthorTableEx author = AuthorTableEx.$;

        executeAndExpectRowCount(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(
                        it -> it.setDialect(new PostgresDialect())
                ).createUpdate(author)
                        .set(author.firstName(), author.firstName().concat("*"))
                        .where(author.books(JoinType.LEFT).store().name().eq("MANNING")),
                ctx -> {
                    ctx.throwable(it -> {
                        it.type(ExecutionException.class);
                        it.message(
                                "The first level table joins cannot be outer join because current dialect " +
                                        "'org.babyfish.jimmer.sql.dialect.PostgresDialect' " +
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
        AdministratorTable table = AdministratorTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createUpdate(table)
                        .set(table.name(), table.name().concat("*"))
                        .where(table.name().ilike("2")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update ADMINISTRATOR tb_1_ " +
                                        "set NAME = concat(tb_1_.NAME, ?) " +
                                        "where tb_1_.NAME ilike ? " +
                                        "and tb_1_.DELETED <> ?"
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
                getSqlClient()
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
                                "delete from BOOK tb_1_ " +
                                        "where tb_1_.ID in (" +
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
                getSqlClient()
                        .createDelete(table)
                        .setMode(DeleteMode.PHYSICAL)
                        .where(table.departmentId().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from EMPLOYEE tb_1_ " +
                                        "where tb_1_.DEPARTMENT_ID = ? " +
                                        "and tb_1_.DELETED_MILLIS = ?"
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
                getSqlClient()
                        .createDelete(table)
                        .where(table.departmentId().eq(1L)),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update EMPLOYEE tb_1_ " +
                                        "set DELETED_MILLIS = ? " +
                                        "where tb_1_.DEPARTMENT_ID = ? and tb_1_.DELETED_MILLIS = ?"
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
                getSqlClient()
                        .createDelete(book)
                        .where(book.store().name().eq("MANNING")),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select distinct tb_1_.ID " +
                                        "from BOOK tb_1_ " +
                                        "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
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

    @Test
    public void updateSqlFormulaFailed() {
        AuthorTable table = AuthorTable.$;
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            getSqlClient()
                    .createUpdate(table)
                    .set(table.fullName2(), "Tim Cook")
                    .where(table.fullName2().eq("Alex Banks"));
        });
        Assertions.assertEquals(
                "The assigned prop expression must be mapped by database columns",
                ex.getMessage()
        );
    }

    @Test
    public void testUpdateForeignKey() {
        EmployeeTable table = EmployeeTable.$;

    }
}
