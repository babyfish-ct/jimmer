package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.babyfish.jimmer.sql.common.Constants.*;

public class SQLiteDMLTest extends AbstractMutationTest {
    @BeforeAll
    public static void beforeAll() {
        jdbc(NativeDatabases.SQLITE_DATA_SOURCE, false, con -> initDatabase(con, "database-sqlite.sql"));
    }

    @Test
    public void testUpdateJoinBySQLite() {
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createUpdate(AuthorTableEx.class, (u, author) -> {
                    u.set(author.firstName(), author.firstName().concat("*"));
                    u.where(author.books().store().name().eq("MANNING"));
                }),
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
    public void testUpdateJoinByPostgresErrorByOuterJoin() {
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createUpdate(AuthorTableEx.class, (u, author) -> {
                    u.set(author.firstName(), author.firstName().concat("*"));
                    u.where(author.books(JoinType.LEFT).store().name().eq("MANNING"));
                }),
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
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createUpdate(BookTable.class, (u, book) -> {
                    u.set(book.name(), book.name().concat("*"));
                    u.where(book.name().ilike("Learning GraphQL"));
                }),
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
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createDelete(BookTable.class, (d, book) -> {
                    d.where(book.name().eq("Learning GraphQL"));
                    d.disableDissociation();
                }),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("delete from BOOK where BOOK.NAME = ?");
                        it.variables("Learning GraphQL");
                    });
                    ctx.rowCount(3);
                }
        );
    }

    @Test
    public void testDeleteWithJoin() {
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createDelete(BookTableEx.class, (d, book) -> {
                    d.where(book.store().name().eq("MANNING"));
                }),
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
