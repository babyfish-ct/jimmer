package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.model.BookTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ForUpdateTest extends AbstractQueryTest {

    @Test
    public void testDefaultRenderPosition() {
        BookTable book = BookTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(book)
                        .where(book.name().eq("Learning GraphQL"))
                        .select(book.id())
                        .forUpdate(),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from BOOK tb_1_ " +
                                    "where tb_1_.NAME = ? for update"
                    );
                    ctx.variables("Learning GraphQL");
                }
        );
    }

    @Test
    public void testSQLiteDoesNotSupportForUpdate() {
        BookTable book = BookTable.$;
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> jdbc(con ->
                        getSqlClient(it -> {
                            it.setDialect(new SQLiteDialect());
                        })
                                .createQuery(book)
                                .select(book.id())
                                .forUpdate()
                                .execute(con)
                )
        );
        Assertions.assertEquals("Sqlite does not support `for update`", ex.getMessage());
    }
}
