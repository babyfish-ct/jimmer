package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.junit.jupiter.api.Test;

import javax.persistence.criteria.JoinType;

public class DMLTest extends AbstractMutationTest {

    @Test
    public void testUpdate() {
        executeAndExpectRowCount(
                getSqlClient().createUpdate(AuthorTable.Ex.class, (u, author) -> {
                    u.set(author.firstName(), author.firstName().concat("*"));
                    u.where(author.firstName().eq("Dan"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "update AUTHOR tb_1 " +
                                        "set FIRST_NAME = concat(tb_1.FIRST_NAME, ?) " +
                                        "where tb_1.FIRST_NAME = ?"
                        );
                        it.variables("*", "Dan");
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testUpdateWithNullArgument() {
        executeAndExpectRowCount(
                getSqlClient().createUpdate(BookStoreTable.Ex.class, (u, store) -> {
                    u.set(store.website(), Expression.nullValue(String.class));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("update BOOK_STORE tb_1 set WEBSITE = null");
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testUpdateJoinByMySql() {

        NativeDatabases.assumeNativeDatabase();

        using(new MySqlDialect(), () -> {
            executeAndExpectRowCount(
                    NativeDatabases.MYSQL_DATA_SOURCE,
                    getSqlClient().createUpdate(AuthorTable.Ex.class, (u, author) -> {
                        u.set(author.firstName(), author.firstName().concat("*"));
                        u.set(author.books().name(), author.books().name().concat("*"));
                        u.set(author.books().store().name(), author.books().store().name().concat("*"));
                        u.where(author.books().store().name().eq("MANNING"));
                    }),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql(
                                    "update AUTHOR tb_1 " +
                                            "inner join BOOK_AUTHOR_MAPPING as tb_2 on tb_1.ID = tb_2.AUTHOR_ID " +
                                            "inner join BOOK as tb_3 on tb_2.BOOK_ID = tb_3.ID " +
                                            "inner join BOOK_STORE as tb_4 on tb_3.STORE_ID = tb_4.ID " +
                                            "set " +
                                            "tb_1.FIRST_NAME = concat(tb_1.FIRST_NAME, ?), " +
                                            "tb_3.NAME = concat(tb_3.NAME, ?), " +
                                            "tb_4.NAME = concat(tb_4.NAME, ?) " +
                                            "where tb_4.NAME = ?"
                            );
                            it.variables("*", "*", "*", "MANNING");
                        });
                    }
            );
        });
    }

    @Test
    public void testUpdateJoinByPostgres() {

        NativeDatabases.assumeNativeDatabase();

        using(new PostgresDialect(), () -> {
            executeAndExpectRowCount(
                    NativeDatabases.POSTGRES_DATA_SOURCE,
                    getSqlClient().createUpdate(AuthorTable.Ex.class, (u, author) -> {
                        u.set(author.firstName(), author.firstName().concat("*"));
                        u.where(author.books().store().name().eq("MANNING"));
                    }),
                    ctx -> {
                        ctx.statement(it -> {
                            it.sql(
                                    "update AUTHOR tb_1 " +
                                            "set FIRST_NAME = concat(tb_1.FIRST_NAME, ?) " +
                                            "from BOOK_AUTHOR_MAPPING as tb_2 " +
                                            "inner join BOOK as tb_3 on tb_2.BOOK_ID = tb_3.ID " +
                                            "inner join BOOK_STORE as tb_4 on " +
                                            "tb_3.STORE_ID = tb_4.ID " +
                                            "where " +
                                            "tb_1.ID = tb_2.AUTHOR_ID " +
                                            "and " +
                                            "tb_4.NAME = ?"
                            );
                            it.variables("*", "MANNING");
                        });
                    }
            );
        });
    }

    @Test
    public void testUpdateJoinByPostgresErrorByOuterJoin() {

        NativeDatabases.assumeNativeDatabase();

        using(new PostgresDialect(), () -> {
            executeAndExpectRowCount(
                    NativeDatabases.POSTGRES_DATA_SOURCE,
                    getSqlClient().createUpdate(AuthorTable.Ex.class, (u, author) -> {
                        u.set(author.firstName(), author.firstName().concat("*"));
                        u.where(author.books(JoinType.LEFT).store().name().eq("MANNING"));
                    }),
                    ctx -> {
                        ctx.throwable(it -> {
                            it.type(ExecutionException.class);
                            it.message(
                                    "The first level table joins cannot be outer join because current dialect " +
                                            "'org.babyfish.jimmer.sql.common.DynamicDialect' " +
                                            "indicates that the first level table joins in update statement must be " +
                                            "rendered as 'from' clause, but there is a first level table join whose " +
                                            "join type is outer: 'Author.books(left)'."
                            );
                        });
                    }
            );
        });
    }
}
