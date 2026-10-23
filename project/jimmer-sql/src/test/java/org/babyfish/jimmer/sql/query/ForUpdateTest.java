package org.babyfish.jimmer.sql.query;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.LockMode;
import org.babyfish.jimmer.sql.ast.query.LockWait;
import org.babyfish.jimmer.sql.ast.query.TypedRootQuery;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;
import java.util.UUID;

public class ForUpdateTest extends AbstractQueryTest {

    @Test
    public void testH2NoWait() {
        BookStoreTable table = BookStoreTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.name().eq("MANNING"))
                        .select(table.id())
                        .forUpdate(LockMode.UPDATE, LockWait.NO_WAIT),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from BOOK_STORE tb_1_ " +
                                    "where tb_1_.NAME = ? " +
                                    "for update nowait"
                    );
                    ctx.rows(1);
                }
        );
    }

    @Test
    public void testPostgresNoWait() {
        executeAndExpect(
                forUpdateQuery(new PostgresDialect(), LockMode.UPDATE, LockWait.NO_WAIT),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for update nowait");
                }
        );
        executeAndExpect(
                forUpdateQuery(new PostgresDialect(), LockMode.SHARE, LockWait.NO_WAIT),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for share nowait");
                }
        );
        executeAndExpect(
                forUpdateQuery(new PostgresDialect(), LockMode.UPDATE, LockWait.SKIP_LOCKED),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for update skip locked");
                }
        );
    }

    @Test
    public void testPostgresWaitSeconds() {
        TypedRootQuery<UUID> query =
                forUpdateQuery(new PostgresDialect(), LockMode.UPDATE, LockWait.seconds(60));
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> jdbc(query::execute)
        );
        Assertions.assertTrue(ex.getMessage().contains("LockWait.seconds(60)"), ex.getMessage());
        Assertions.assertTrue(ex.getMessage().contains("lock_timeout"), ex.getMessage());
    }

    @Test
    public void testOracle() {
        executeAndExpect(
                forUpdateQuery(new OracleDialect(), LockMode.UPDATE, LockWait.DEFAULT),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for update");
                }
        );
        executeAndExpect(
                forUpdateQuery(new OracleDialect(), LockMode.UPDATE, LockWait.NO_WAIT),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for update nowait");
                }
        );
        executeAndExpect(
                forUpdateQuery(new OracleDialect(), LockMode.UPDATE, LockWait.SKIP_LOCKED),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for update skip locked");
                }
        );
        executeAndExpect(
                forUpdateQuery(new OracleDialect(), LockMode.UPDATE, LockWait.seconds(60)),
                ctx -> {
                    ctx.sql("select tb_1_.ID from BOOK_STORE tb_1_ for update wait 60");
                }
        );
    }

    private TypedRootQuery<UUID> forUpdateQuery(Dialect dialect, LockMode lockMode, LockWait lockWait) {
        BookStoreTable table = BookStoreTable.$;
        return sqlOnlyClient(dialect)
                .createQuery(table)
                .select(table.id())
                .forUpdate(lockMode, lockWait);
    }

    private JSqlClient sqlOnlyClient(Dialect dialect) {
        return getSqlClient(it -> {
            it.setDialect(dialect);
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(@NotNull Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    return (R) Collections.emptyList();
                }

                @Override
                public BatchContext executeBatch(
                        @NotNull Connection con,
                        @NotNull String sql,
                        @Nullable ImmutableProp generatedIdProp,
                        @NotNull ExecutionPurpose purpose,
                        @NotNull JSqlClientImplementor sqlClient,
                        boolean constraintViolationTranslatable
                ) {
                    throw new AssertionError("Batch execution is not expected");
                }
            });
        });
    }
}
