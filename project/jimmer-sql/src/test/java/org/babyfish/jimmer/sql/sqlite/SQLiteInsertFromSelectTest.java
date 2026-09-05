package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.UUID;

import static org.babyfish.jimmer.sql.common.Constants.learningGraphQLId1;
import static org.babyfish.jimmer.sql.common.Constants.oreillyId;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLiteInsertFromSelectTest extends AbstractMutationTest {

    @BeforeAll
    public static void beforeAll() {
        jdbc(
                NativeDatabases.SQLITE_DATA_SOURCE,
                false,
                con -> initDatabase(con, "database-sqlite.sql")
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void sqliteComputedMergeSource(boolean computed) throws Exception {
        JSqlClient client = getSqlClient(it -> it.setDialect(new SQLiteDialect()));
        BookStoreTable table = BookStoreTable.$;
        BaseTable1<NumericExpression<Integer>> source = client.createBaseQuery().addSelect(Expression.constant(1)).asBaseTable();
        try (Connection con = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (Statement stmt = con.createStatement()) {
                stmt.execute("create table BOOK_STORE(ID text primary key, NAME text, WEBSITE text, VERSION integer)");
                stmt.execute("insert into BOOK_STORE values('" + oreillyId + "', 'OReilly', null, 0)");
            }
            assertEquals(1, client.createUpsert(table, source)
                    .key(table.id(), Expression.value(oreillyId))
                    .merge(table.version(), computed ? source.get_1().plus(1) : source.get_1())
                    .execute(con));
            assertEquals(Collections.singletonList(computed ? 2 : 1), client.createQuery(table)
                    .where(table.id().eq(oreillyId)).select(table.version()).execute(con));
        }
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
