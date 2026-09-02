package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId1;

public class SQLiteInsertFromSelectTest extends AbstractMutationTest {

    @BeforeAll
    public static void beforeAll() {
        jdbc(
                NativeDatabases.SQLITE_DATA_SOURCE,
                false,
                con -> initDatabase(con, "database-sqlite.sql")
        );
    }

    @Test
    public void testConflictClauseAfterWrappedSourceSelect() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new SQLiteDialect()));
        BookTable book = BookTable.$;
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = sqlClient
                .createBaseQuery(book)
                .where(book.id().eq(learningGraphQLId1))
                .addSelect(book.storeId())
                .addSelect(book.name())
                .asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                NativeDatabases.SQLITE_DATA_SOURCE,
                sqlClient
                        .createUpsert(store, source)
                        .key(store.id(), source.get_1())
                        .merge(store.name(), source.get_2()),
                ctx -> {
                    ctx.statement(it -> {
                    });
                    ctx.rowCount(1);
                }
        );
    }
}
