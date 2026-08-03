package org.babyfish.jimmer.sql.sqlserver;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;

public class SqlServerQueryTest extends AbstractQueryTest {

    @Test
    public void testForUpdateWithOrderBy() {
        BookTable book = BookTable.$;
        executeAndExpect(
                sqlOnlyClient()
                        .createQuery(book)
                        .where(book.name().eq("Learning GraphQL"))
                        .orderBy(book.id())
                        .select(book.id())
                        .forUpdate(),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from BOOK tb_1_ with(updlock, rowlock) " +
                                    "where tb_1_.NAME = ? order by tb_1_.ID asc"
                    );
                    ctx.variables("Learning GraphQL");
                }
        );
    }

    private JSqlClient sqlOnlyClient() {
        return getSqlClient(it -> {
            it.setDialect(new SqlServerDialect());
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    return (R) Collections.emptyList();
                }

                @Override
                public BatchContext executeBatch(
                        Connection con,
                        String sql,
                        ImmutableProp generatedIdProp,
                        ExecutionPurpose purpose,
                        JSqlClientImplementor sqlClient,
                        boolean constraintViolationTranslatable
                ) {
                    throw new AssertionError("Batch execution is not expected");
                }
            });
        });
    }
}
