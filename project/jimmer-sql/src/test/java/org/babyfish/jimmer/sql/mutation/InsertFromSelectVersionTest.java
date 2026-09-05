package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.*;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.model.BookStore;
import org.babyfish.jimmer.sql.model.BookStoreTable;
import org.babyfish.jimmer.sql.model.BookTable;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InsertFromSelectVersionTest extends AbstractMutationTest {

    @Test
    public void testImplicitVersionIsEquivalentForNativeAndMaterializedPlans() {
        JSqlClient nativeClient = getSqlClient();
        JSqlClient materializedClient = materializedH2Client();
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> {
            setStoreVersion(con, oreillyId, 5);
            setStoreVersion(con, manningId, 5);
            clearExecutions();

            BaseTable2<ComparableExpression<UUID>, StringExpression> nativeSource = nameSource(
                    nativeClient,
                    oreillyId,
                    "-NATIVE"
            );
            BaseTable2<ComparableExpression<UUID>, StringExpression> materializedSource = nameSource(
                    materializedClient,
                    manningId,
                    "-MATERIALIZED"
            );
            List<Integer> nativeVersions = nativeClient
                    .createUpsert(table, nativeSource)
                    .key(table.id(), nativeSource.get_1())
                    .merge(table.name(), nativeSource.get_2())
                    .returning(table.version())
                    .execute(con);
            List<Integer> materializedVersions = materializedClient
                    .createUpsert(table, materializedSource)
                    .key(table.id(), materializedSource.get_1())
                    .merge(table.name(), materializedSource.get_2())
                    .returning(table.version())
                    .execute(con);

            assertEquals(singletonList(5), nativeVersions);
            assertEquals(nativeVersions, materializedVersions);
            assertEquals(asList("O'REILLY-NATIVE", 5), storeNameAndVersion(con, oreillyId));
            assertEquals(asList("MANNING-MATERIALIZED", 5), storeNameAndVersion(con, manningId));
            assertExecutedSql(
                    "select VERSION from final table (" +
                            "merge into BOOK_STORE tb_2_ using (" +
                            "select tb_1_.ID c1, concat(tb_1_.NAME, ?) c2 " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_ on tb_2_.ID = tb_1_.c1 " +
                            "when matched then update set tb_2_.NAME = tb_1_.c2 " +
                            "when not matched then insert(ID, NAME, VERSION) " +
                            "values(tb_1_.c1, tb_1_.c2, ?))",
                    "select tb_1_.c1, tb_1_.c2, ? from (" +
                            "select tb_1_.ID c1, concat(tb_1_.NAME, ?) c2 " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "select ID, VERSION from final table (merge into BOOK_STORE tb_1_ " +
                            "using(values(?, ?, ?)) tb_2_(ID, NAME, VERSION) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when matched then update set NAME = tb_2_.NAME " +
                            "when not matched then insert(ID, NAME, VERSION) " +
                            "values(tb_2_.ID, tb_2_.NAME, tb_2_.VERSION))"
            );
        });
    }

    @Test
    public void testMaterializedInsertOnlyVersionDoesNotChangeMatchedRow() {
        JSqlClient sqlClient = materializedH2Client();
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> {
            setStoreVersion(con, oreillyId, 5);
            clearExecutions();

            BaseTable2<ComparableExpression<UUID>, NumericExpression<Integer>> source = versionSource(
                    sqlClient,
                    oreillyId,
                    4
            );
            List<Integer> versions = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.get_1())
                    .insert(table.version(), source.get_2())
                    .returning(table.version())
                    .execute(con);

            assertEquals(singletonList(5), versions);
            assertEquals(asList("O'REILLY", 5), storeNameAndVersion(con, oreillyId));
            assertExecutedSql(
                    "select tb_1_.c1, tb_1_.c2 from (" +
                            "select tb_1_.ID c1, tb_1_.VERSION + ? c2 " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "select ID, VERSION from final table (merge into BOOK_STORE tb_1_ " +
                            "using(values(?, ?)) tb_2_(ID, VERSION) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when matched then update set " +
                            "/* fake update to return all ids */ VERSION = tb_1_.VERSION " +
                            "when not matched then insert(ID, VERSION) " +
                            "values(tb_2_.ID, tb_2_.VERSION))"
            );
        });
    }

    @Test
    public void testMaterializedMergeVersionWritesSourceValue() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> {
            setStoreVersion(con, oreillyId, 5);
            clearExecutions();

            BaseTable2<ComparableExpression<UUID>, NumericExpression<Integer>> source = versionSource(
                    sqlClient,
                    oreillyId,
                    4
            );
            List<Integer> versions = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.get_1())
                    .merge(table.version(), source.get_2())
                    .returning(table.version())
                    .execute(con);

            assertEquals(singletonList(9), versions);
            assertEquals(asList("O'REILLY", 9), storeNameAndVersion(con, oreillyId));
            assertExecutedSql(
                    "select tb_1_.c1, tb_1_.c2 from (" +
                            "select tb_1_.ID c1, tb_1_.VERSION + ? c2 " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?",
                    "update BOOK_STORE set VERSION = ? where ID = ?"
            );
        });
    }

    @Test
    public void testMaterializedFakeUpsertEventKeepsDatabaseVersion() {
        JSqlClient sqlClient = getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY));
        AtomicReference<EntityEvent<?>> eventRef = new AtomicReference<>();
        sqlClient.getTriggers(true).addEntityListener(BookStore.class, eventRef::set);
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> {
            setStoreVersion(con, oreillyId, 5);
            clearExecutions();

            BaseTable2<ComparableExpression<UUID>, NumericExpression<Integer>> source = versionSource(
                    sqlClient,
                    oreillyId,
                    4
            );
            int affectedRowCount = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.get_1())
                    .insert(table.version(), source.get_2())
                    .execute(con);

            assertEquals(1, affectedRowCount);
            EntityEvent<?> event = eventRef.get();
            assertNotNull(event);
            assertEquals(5, ((BookStore) event.getOldEntity()).version());
            assertEquals(5, ((BookStore) event.getNewEntity()).version());
            assertEquals(asList("O'REILLY", 5), storeNameAndVersion(con, oreillyId));
            assertExecutedSql(
                    "select tb_1_.c1, tb_1_.c2 from (" +
                            "select tb_1_.ID c1, tb_1_.VERSION + ? c2 " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.WEBSITE, tb_1_.VERSION " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?",
                    "update BOOK_STORE set " +
                            "/* fake update to return all ids */ VERSION = VERSION " +
                            "where ID = ?"
            );
        });
    }

    @Test
    public void testMySqlMaterializedFakeUpsertReturningDoesNotDependOnChangedRowCount() {
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    R result = DefaultExecutor.INSTANCE.execute(args);
                    if (args.sql.startsWith("update BOOK_STORE set /* fake update to return all ids */")) {
                        return (R) Integer.valueOf(0);
                    }
                    return result;
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
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> {
            clearExecutions();
            BaseTable1<ComparableExpression<UUID>> source = sqlClient
                    .createBaseQuery(table)
                    .where(table.id().eq(oreillyId))
                    .addSelect(table.id())
                    .asBaseTable();
            List<UUID> ids = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.get_1())
                    .returning(table.id())
                    .execute(con);

            assertEquals(singletonList(oreillyId), ids);
            assertExecutedSql(
                    "select tb_1_.c1, ? from (" +
                            "select tb_1_.ID c1 from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?",
                    "update BOOK_STORE set " +
                            "/* fake update to return all ids */ VERSION = VERSION " +
                            "where ID = ?"
            );
        });
    }

    @Test
    public void testMaterializedCustomMergeCanCheckAndIncreaseVersion() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BookStoreTable table = BookStoreTable.$;

        jdbc(con -> {
            setStoreVersion(con, oreillyId, 5);
            clearExecutions();

            BaseTable2<ComparableExpression<UUID>, NumericExpression<Integer>> source = versionSource(
                    sqlClient,
                    oreillyId,
                    0
            );
            List<Integer> versions = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.get_1())
                    .merge(
                            table.version(),
                            source.get_2(),
                            table.version().plus(1)
                    )
                    .updateWhere(source.get_2().eq(table.version()))
                    .returning(table.version())
                    .execute(con);

            assertEquals(singletonList(6), versions);
            assertEquals(asList("O'REILLY", 6), storeNameAndVersion(con, oreillyId));
            assertExecutedSql(
                    "select tb_1_.c1, tb_1_.c2 from (" +
                            "select tb_1_.ID c1, tb_1_.VERSION c2 " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?",
                    "select tb_1_.ID from BOOK_STORE tb_1_ " +
                            "where tb_1_.ID = ? and ? = tb_1_.VERSION",
                    "update BOOK_STORE set VERSION = VERSION + ? " +
                            "where ID = ? and ? = VERSION",
                    "select tb_1_.ID, tb_1_.VERSION " +
                            "from BOOK_STORE tb_1_ where tb_1_.ID = ?"
            );
        });
    }

    @Test
    public void testMaterializedFakeUpsertWithoutVersionIsAccepted() {
        JSqlClient sqlClient = materializedH2Client();
        BookTable table = BookTable.$;

        jdbc(con -> {
            clearExecutions();
            BaseTable1<ComparableExpression<UUID>> source = sqlClient
                    .createBaseQuery(table)
                    .where(table.id().eq(effectiveTypeScriptId1))
                    .addSelect(table.id())
                    .asBaseTable();
            List<UUID> ids = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.get_1())
                    .returning(table.id())
                    .execute(con);

            assertEquals(singletonList(effectiveTypeScriptId1), ids);
            assertExecutedSql(
                    "select tb_1_.c1 from (" +
                            "select tb_1_.ID c1 from BOOK tb_1_ where tb_1_.ID = ?" +
                            ") tb_1_",
                    "merge into BOOK tb_1_ using(values(?)) tb_2_(ID) " +
                            "on tb_1_.ID = tb_2_.ID " +
                            "when matched then update set " +
                            "/* fake update to return all ids */ PRICE = tb_1_.PRICE " +
                            "when not matched then insert(ID) values(tb_2_.ID)"
            );
        });
    }

    private static BaseTable2<ComparableExpression<UUID>, StringExpression> nameSource(
            JSqlClient sqlClient,
            UUID id,
            String name
    ) {
        BookStoreTable table = BookStoreTable.$;
        return sqlClient
                .createBaseQuery(table)
                .where(table.id().eq(id))
                .addSelect(table.id())
                .addSelect(table.name().concat(name))
                .asBaseTable();
    }

    private JSqlClient materializedH2Client() {
        return getSqlClient(it -> it.setDialect(new H2Dialect() {
            private final InsertFromSelectRenderer renderer = new InsertFromSelectRenderer() {
                @Override
                public boolean isSupported(InsertFromSelectContext ctx) {
                    return false;
                }

                @Override
                public void render(InsertFromSelectContext ctx) {
                    throw new AssertionError("The unsupported renderer must not be invoked");
                }
            };

            @Override
            public InsertFromSelectRenderer getInsertFromSelectRenderer() {
                return renderer;
            }
        }));
    }

    private static BaseTable2<ComparableExpression<UUID>, NumericExpression<Integer>> versionSource(
            JSqlClient sqlClient,
            UUID id,
            int increment
    ) {
        BookStoreTable table = BookStoreTable.$;
        return sqlClient
                .createBaseQuery(table)
                .where(table.id().eq(id))
                .addSelect(table.id())
                .addSelect(increment != 0 ? table.version().plus(increment) : table.version())
                .asBaseTable();
    }

    private static void setStoreVersion(Connection con, UUID id, int version) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "update BOOK_STORE set VERSION = ? where ID = ?"
        )) {
            stmt.setInt(1, version);
            stmt.setObject(2, id);
            assertEquals(1, stmt.executeUpdate());
        }
    }

    private static List<Object> storeNameAndVersion(Connection con, UUID id) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(
                "select NAME, VERSION from BOOK_STORE where ID = ?"
        )) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return asList(rs.getString(1), rs.getInt(2));
            }
        }
    }

    private void assertExecutedSql(String... expectedSql) {
        List<String> actualSql = new ArrayList<>();
        getExecutions().forEach(it -> actualSql.add(it.getSql()));
        assertEquals(asList(expectedSql), actualSql);
    }
}
