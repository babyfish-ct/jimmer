package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
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
                                "update AUTHOR tb_1_ " +
                                        "set FIRST_NAME = concat(tb_1_.FIRST_NAME, ?) " +
                                        "where tb_1_.FIRST_NAME = ?"
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
                        it.sql("update BOOK_STORE tb_1_ set WEBSITE = null");
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
                                    "update AUTHOR tb_1_ " +
                                            "inner join BOOK_AUTHOR_MAPPING as tb_2_ on tb_1_.ID = tb_2_.AUTHOR_ID " +
                                            "inner join BOOK as tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                            "inner join BOOK_STORE as tb_4_ on tb_3_.STORE_ID = tb_4_.ID " +
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
                                    "update AUTHOR tb_1_ " +
                                            "set FIRST_NAME = concat(tb_1_.FIRST_NAME, ?) " +
                                            "from BOOK_AUTHOR_MAPPING as tb_2_ " +
                                            "inner join BOOK as tb_3_ on tb_2_.BOOK_ID = tb_3_.ID " +
                                            "inner join BOOK_STORE as tb_4_ on " +
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

    @Test
    public void testDelete() {
        executeAndExpectRowCount(
                getSqlClient().createDelete(BookTable.Ex.class, (d, book) -> {
                    d.where(book.name().eq("Learning GraphQL"));
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK as tb_1_ where tb_1_.NAME = ?");
                        it.variables("Learning GraphQL");
                    });
                    ctx.rowCount(3);
                }
        );
    }
}
