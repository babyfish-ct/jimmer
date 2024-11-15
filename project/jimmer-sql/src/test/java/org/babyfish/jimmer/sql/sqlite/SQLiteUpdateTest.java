package org.babyfish.jimmer.sql.sqlite;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.AuthorTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.babyfish.jimmer.sql.common.Constants.sammerId;

public class SQLiteUpdateTest extends AbstractMutationTest {
    @BeforeAll
    public static void beforeAll() {
        DataSource dataSource = NativeDatabases.SQLITE_DATA_SOURCE;
        jdbc(dataSource, false, con -> initDatabase(con, "database-sqlite.sql"));
    }

    @Test
    public void test() {
        executeAndExpectRowCount(
                getLambdaClient(
                        it -> it.setDialect(new SQLiteDialect())
                ).createUpdate(AuthorTable.class, (u, author) -> {
                    u.set(author.lastName(), "Sammer");
                    u.where(author.id().eq(sammerId));
                }),
                cxt -> {
                    cxt.statement(it -> {
                        it.sql("update AUTHOR set LAST_NAME = ? where AUTHOR.ID = ?");
                        it.variables("Sammer", sammerId);
                    });
                    cxt.rowCount(1);
                });
    }
}
