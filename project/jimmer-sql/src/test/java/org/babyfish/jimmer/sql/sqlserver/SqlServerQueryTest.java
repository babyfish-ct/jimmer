package org.babyfish.jimmer.sql.sqlserver;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.query.LockMode;
import org.babyfish.jimmer.sql.ast.query.LockWait;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.model.inheritance.joinedtable.OrganizationTable;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Assertions;
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

    @Test
    public void testForUpdateWithJoin() {
        BookTable book = BookTable.$;
        executeAndExpect(
                sqlOnlyClient()
                        .createQuery(book)
                        .where(book.store().name().eq("MANNING"))
                        .select(book.id())
                        .forUpdate(),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from BOOK tb_1_ with(updlock, rowlock) " +
                                    "inner join BOOK_STORE tb_2_ on tb_1_.STORE_ID = tb_2_.ID " +
                                    "where tb_2_.NAME = ?"
                    );
                    ctx.variables("MANNING");
                }
        );
    }

    @Test
    public void testForUpdateDoesNotLeakIntoSubQuery() {
        JSqlClient sqlClient = sqlOnlyClient();
        BookStoreTable store = BookStoreTable.$;
        BookTable book = BookTable.$;
        executeAndExpect(
                sqlClient
                        .createQuery(store)
                        .where(
                                sqlClient
                                        .createSubQuery(book)
                                        .where(
                                                book.store().eq(store),
                                                book.name().eq("Learning GraphQL")
                                        )
                                        .select(book.id())
                                        .exists()
                        )
                        .select(store.id())
                        .forUpdate(),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from BOOK_STORE tb_1_ with(updlock, rowlock) " +
                                    "where exists(" +
                                    "select 1 from BOOK tb_2_ " +
                                    "where tb_2_.STORE_ID = tb_1_.ID and tb_2_.NAME = ?" +
                                    ")"
                    );
                    ctx.variables("Learning GraphQL");
                }
        );
    }

    @Test
    public void testForUpdateWithSkipLocked() {
        BookTable book = BookTable.$;
        executeAndExpect(
                sqlOnlyClient()
                        .createQuery(book)
                        .where(book.name().eq("Learning GraphQL"))
                        .select(book.id())
                        .forUpdate(LockMode.UPDATE, LockWait.SKIP_LOCKED),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID from BOOK tb_1_ with(updlock, rowlock, readpast) " +
                                    "where tb_1_.NAME = ?"
                    );
                    ctx.variables("Learning GraphQL");
                }
        );
    }

    @Test
    public void testForUpdateWithJoinedInheritanceRoot() {
        OrganizationTable organization = OrganizationTable.$;
        executeAndExpect(
                sqlOnlyClient()
                        .createQuery(organization)
                        .where(organization.id().eq(200L))
                        .select(organization.id(), organization.taxCode())
                        .forUpdate(),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1__sub.TAX_CODE " +
                                    "from JOINED_CLIENT tb_1_ with(updlock, rowlock) " +
                                    "inner join JOINED_ORGANIZATION tb_1__sub " +
                                    "on tb_1_.ID = tb_1__sub.ID " +
                                    "where tb_1_.ID = ? and tb_1_.CLIENT_TYPE = ?"
                    );
                    ctx.variables(200L, "ORG");
                }
        );
    }

    @Test
    public void testForUpdateWithCteRoot() {
        JSqlClient sqlClient = sqlOnlyClient();
        BookTable book = BookTable.$;
        BaseTable1<BookTable> cte = sqlClient
                .createBaseQuery(book)
                .addSelect(book)
                .asCteBaseTable();
        executeAndExpect(
                sqlClient
                        .createQuery(cte)
                        .select(cte.get_1().id())
                        .forUpdate(),
                ctx -> ctx.sql(
                        "with tb_1_(c1) as (" +
                                "select tb_2_.ID from BOOK tb_2_" +
                                ") " +
                                "select tb_1_.c1 from tb_1_ with(updlock, rowlock)"
                )
        );
    }

    @Test
    public void testForUpdateWithDerivedTableRootIsRejected() {
        JSqlClient sqlClient = sqlOnlyClient();
        BookTable book = BookTable.$;
        BaseTable1<BookTable> derivedTable = sqlClient
                .createBaseQuery(book)
                .addSelect(book)
                .asBaseTable();
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> jdbc(con ->
                        sqlClient
                                .createQuery(derivedTable)
                                .select(derivedTable.get_1().id())
                                .forUpdate()
                                .execute(con)
                )
        );
        Assertions.assertEquals(
                "For update at table alias suffix is not supported for derived tables",
                ex.getMessage()
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
