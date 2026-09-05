package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.query.ConfigurableBaseQuery;
import org.babyfish.jimmer.sql.ast.query.TypedBaseQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable1;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable4;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.dialect.*;
import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.exception.ExecutionException;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.*;
import org.babyfish.jimmer.sql.model.embedded.MachineTable;
import org.babyfish.jimmer.sql.model.embedded.ProductTable;
import org.babyfish.jimmer.sql.model.hr.DepartmentTable;
import org.babyfish.jimmer.sql.model.inheritance.RoleTable;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.tuple.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.babyfish.jimmer.sql.common.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class InsertFromSelectTest extends AbstractMutationTest {

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyPreservesDatabaseDefaultOnInsert(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        SysUserTable table = SysUserTable.$;
        jdbc(con -> {
            assertEquals(singletonList("DEFAULT_DESCRIPTION"), client.createUpsert(table, singleRowSource(client))
                    .update(table.description(), table.description().concat("!"))
                    .key(table.id(), Expression.value(100L))
                    .insert(table.account(), Expression.value("update_only"))
                    .insert(table.email(), Expression.value("update_only@example.org"))
                    .insert(table.area(), Expression.value("north"))
                    .insert(table.nickName(), Expression.value("Update only"))
                    .returning(table.description())
                    .execute(con));
            assertEquals(singletonList("DEFAULT_DESCRIPTION"), client.createQuery(table)
                    .where(table.id().eq(100L)).select(table.description()).execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyReadsTargetAndUninsertedSource(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        BookStoreTable table = BookStoreTable.$;
        BaseTable2<NumericExpression<Integer>, StringExpression> source = client.createBaseQuery()
                .addSelect(Expression.value(3)).addSelect(Expression.value("UPDATED")).asBaseTable();
        jdbc(con -> {
            assertEquals(singletonList(new Tuple3<>(oreillyId, "UPDATED", 3)), client.createUpsert(table, source)
                    .update(table.website(), source.get_2())
                    .key(table.id(), Expression.value(oreillyId))
                    .insert(table.name(), Expression.value("INSERT-ONLY"))
                    .update(table.version(), table.version().plus(source.get_1()))
                    .returning(table.id(), table.website(), table.version())
                    .execute(con));
            assertEquals(singletonList("O'REILLY"), client.createQuery(table)
                    .where(table.id().eq(oreillyId)).select(table.name()).execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyEmbeddedMember(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        MachineTable table = MachineTable.$;
        jdbc(con -> {
            assertEquals(singletonList(new Tuple2<>("localhost", 8081)), client.createUpsert(table, singleRowSource(client))
                    .update(table.location().port(), table.location().port().plus(1))
                    .key(table.id(), Expression.value(1L))
                    .insert(table.location().host(), Expression.value("INSERT-ONLY"))
                    .returning(table.location().host(), table.location().port())
                    .execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyCanBeRejectedByUpdateWhere(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            assertEquals(emptyList(), client.createUpsert(table, singleRowSource(client))
                    .key(table.id(), Expression.value(oreillyId))
                    .update(table.website(), Expression.value("REJECTED"))
                    .updateWhere(table.name().eq("OTHER"))
                    .returning(table.id())
                    .execute(con));
            assertEquals(singletonList(null), client.createQuery(table)
                    .where(table.id().eq(oreillyId)).select(table.website()).execute(con));
        });
    }

    private JSqlClient updateOnlyClient(String plan) {
        switch (plan) {
            case "native":
                return getSqlClient();
            case "save":
                return getSqlClient(it -> it.setDialect(new H2Dialect() {
                    @Override
                    public InsertFromSelectRenderer getInsertFromSelectRenderer() {
                        return DefaultDialect.INSTANCE.getInsertFromSelectRenderer();
                    }
                }));
            case "emulated":
                return getSqlClient(it -> it.setDialect(new H2Dialect() {
                    @Override
                    public InsertFromSelectRenderer getInsertFromSelectRenderer() {
                        return DefaultDialect.INSTANCE.getInsertFromSelectRenderer();
                    }

                    @Override
                    public boolean isUpsertSupported() {
                        return false;
                    }
                }));
            default:
                throw new AssertionError("Unexpected execution plan: " + plan);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyVersionIsInitializedOnInsert(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> assertEquals(singletonList(0), client.createUpsert(table, singleRowSource(client))
                .key(table.id(), Expression.value(UUID.fromString("a0000000-0000-0000-0000-000000000099")))
                .insert(table.name(), Expression.value("UPDATE-ONLY-VERSION"))
                .update(table.version(), table.version().plus(5))
                .updateWhere(table.version().lt(0))
                .returning(table.version())
                .execute(con)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyNullableValue(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            client.createUpdate(table).set(table.website(), "BEFORE").where(table.id().eq(oreillyId)).execute(con);
            assertEquals(singletonList(null), client.createUpsert(table, singleRowSource(client))
                    .update(table.website(), Expression.nullValue(String.class))
                    .key(table.id(), Expression.value(oreillyId))
                    .returning(table.website())
                    .execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "save", "emulated"})
    public void testUpdateOnlyReferenceId(String plan) {
        JSqlClient client = updateOnlyClient(plan);
        BookTable table = BookTable.$;
        jdbc(con -> assertEquals(singletonList(manningId), client.createUpsert(table, singleRowSource(client))
                .update(table.storeId(), Expression.value(manningId))
                .key(table.id(), Expression.value(learningGraphQLId1))
                .returning(table.storeId())
                .execute(con)));
    }

    @Test
    public void testDialectRendererDelegation() {
        AtomicBoolean supported = new AtomicBoolean();
        AtomicBoolean rendered = new AtomicBoolean();
        AtomicReference<InsertFromSelectMode> modeRef = new AtomicReference<>();
        InsertFromSelectRenderer delegate = new H2Dialect().getInsertFromSelectRenderer();
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new H2Dialect() {
            private final InsertFromSelectRenderer renderer = new InsertFromSelectRenderer() {
                @Override
                public boolean isSupported(InsertFromSelectContext ctx) {
                    supported.set(true);
                    modeRef.set(ctx.getMode());
                    return delegate.isSupported(ctx);
                }

                @Override
                public boolean isTransactionTriggerSupported(InsertFromSelectContext ctx) {
                    return delegate.isTransactionTriggerSupported(ctx);
                }

                @Override
                public void render(InsertFromSelectContext ctx) {
                    rendered.set(true);
                    delegate.render(ctx);
                }
            };

            @Override
            public InsertFromSelectRenderer getInsertFromSelectRenderer() {
                return renderer;
            }
        }));
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000021");
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = sqlClient
                .createBaseQuery()
                .addSelect(Expression.value(id))
                .addSelect(Expression.value("RENDERER"))
                .asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                sqlClient
                        .createInsert(store, source)
                        .set(store.id(), source.get_1())
                        .set(store.name(), source.get_2()),
                ctx -> {
                    ctx.statement(it -> {
                    });
                    ctx.rowCount(1);
                }
        );
        assertTrue(supported.get());
        assertTrue(rendered.get());
        assertEquals(InsertFromSelectMode.INSERT, modeRef.get());
    }

    @Test
    public void testInsertFromNormalTypedBaseQuery() {
        BookTable book = BookTable.$;
        BookUpdateReturningTupleTable source = getSqlClient()
                .createBaseQuery(book)
                .where(book.id().eq(learningGraphQLId1))
                .select(
                        BookUpdateReturningTupleMapper
                                .id(book.id())
                                .name(book.name().concat("+"))
                )
                .asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(store, source)
                        .set(store.id(), source.getId())
                        .set(store.name(), source.getName()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_STORE(ID, NAME, VERSION) " +
                                        "select tb_1_.c1, tb_1_.c2, ? from (" +
                                        "select tb_1_.ID c1, concat(tb_1_.NAME, ?) c2 " +
                                        "from BOOK tb_1_ where tb_1_.ID = ?" +
                                        ") tb_1_"
                        );
                        it.variables(0, "+", learningGraphQLId1);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testInsertFromRootlessTypedUnionAll() {
        UUID id1 = UUID.fromString("a0000000-0000-0000-0000-000000000011");
        UUID id2 = UUID.fromString("a0000000-0000-0000-0000-000000000012");
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> row1 = getSqlClient()
                .createBaseQuery()
                .select(
                        BookUpdateReturningTupleMapper
                                .id(Expression.value(id1))
                                .name(Expression.value("UNION-1"))
                );
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> row2 = getSqlClient()
                .createBaseQuery()
                .select(
                        BookUpdateReturningTupleMapper
                                .id(Expression.value(id2))
                                .name(Expression.value("UNION-2"))
                );
        BookUpdateReturningTupleTable source = TypedBaseQuery.unionAll(row1, row2).asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(store, source)
                        .set(store.id(), source.getId())
                        .set(store.name(), source.getName()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "insert into BOOK_STORE(ID, NAME, VERSION) " +
                                    "select tb_1_.c1, tb_1_.c2, ? from (" +
                                    "select cast(? as char(36)) as c1, cast(? as varchar) as c2 " +
                                    "union all " +
                                    "select cast(? as char(36)) as c1, cast(? as varchar) as c2" +
                                    ") tb_1_"
                    ));
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testWideTypedTupleSourcePruning() {
        RoleTable role = RoleTable.$;
        WideTupleTable source = getSqlClient()
                .createBaseQuery(role)
                .where(role.id().eq(100L))
                .select(
                        WideTupleMapper
                                .value1(Expression.constant(1L))
                                .value2(Expression.constant(2L))
                                .value3(Expression.constant(3L))
                                .value4(Expression.constant(4L))
                                .value5(Expression.constant(5L))
                                .value6(Expression.constant(6L))
                                .value7(Expression.constant(7L))
                                .value8(Expression.constant(8L))
                                .value9(Expression.constant(9L))
                                .value10(role.id())
                )
                .asBaseTable();
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(role, source)
                        .set(role.id(), source.getValue10())
                        .onConflictDoNothing(role.id()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into ROLE tb_2_ using (" +
                                        "select tb_1_.ID c1 from ROLE tb_1_ " +
                                        "where tb_1_.ID = ? and tb_1_.DELETED <> ?" +
                                        ") tb_1_ on tb_2_.ID = tb_1_.c1 " +
                                        "when not matched then insert(ID, DELETED) values(tb_1_.c1, ?)"
                        );
                        it.variables(100L, true, false);
                    });
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testMaterializedReturningWithGeneratedId() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BookTable book = BookTable.$;
        BookUpdateReturningTupleTable source = sqlClient
                .createBaseQuery(book)
                .where(book.id().eq(learningGraphQLId1))
                .select(
                        BookUpdateReturningTupleMapper
                                .id(book.id())
                                .name(book.name().concat("-MATERIALIZED"))
                )
                .asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        jdbc(con -> {
            java.util.List<UUID> ids = sqlClient
                    .createInsert(store, source)
                    .set(store.name(), source.getName())
                    .returning(store.id())
                    .execute(con);
            assertEquals(1, ids.size());
            assertNotNull(ids.get(0));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void returningInsertOnlyValueOnMatchedRow(boolean fallback) {
        JSqlClient client = client(fallback);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            assertEquals(singletonList("O'REILLY"), client.createUpsert(table, singleRowSource(client))
                    .key(table.id(), Expression.value(oreillyId))
                    .insert(table.name(), Expression.value("INSERT-ONLY"))
                    .merge(table.website(), Expression.value("updated"))
                    .returning(table.name())
                    .execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void materializedReturningThroughNativeSaveUpsert(boolean returningSupported) {
        JSqlClient client = getSqlClient(it -> it.setDialect(new H2Dialect() {
            @Override
            public boolean isUpsertReturningSupported() {
                return returningSupported;
            }

            @Override
            public InsertFromSelectRenderer getInsertFromSelectRenderer() {
                return DefaultDialect.INSTANCE.getInsertFromSelectRenderer();
            }
        }));
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> assertEquals(singletonList("O'REILLY"), client.createUpsert(table, singleRowSource(client))
                .key(table.id(), Expression.value(oreillyId))
                .insert(table.name(), Expression.value("INSERT-ONLY"))
                .merge(table.website(), Expression.value("updated"))
                .returning(table.name())
                .execute(con)));
    }

    @Test
    public void testMaterializedReturningWithDatabaseDefault() {
        resetIdentity(null, "SYS_USER");
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(DefaultDialect.INSTANCE);
            it.setIdGenerator(IdentityIdGenerator.INSTANCE);
        });
        SysUserTable sourceUser = SysUserTable.$;
        BaseTable4<StringExpression, StringExpression, StringExpression, StringExpression> source = sqlClient
                .createBaseQuery(sourceUser)
                .where(sourceUser.id().eq(1L))
                .addSelect(sourceUser.account().concat("_m"))
                .addSelect(sourceUser.email().concat(".m"))
                .addSelect(sourceUser.area().concat("m"))
                .addSelect(sourceUser.nickName().concat("M"))
                .asBaseTable();
        SysUserTable table = SysUserTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createInsert(table, source)
                        .set(table.account(), source.get_1())
                        .set(table.email(), source.get_2())
                        .set(table.area(), source.get_3())
                        .set(table.nickName(), source.get_4())
                        .returning(table.description())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.c1, tb_1_.c2, tb_1_.c3, tb_1_.c4 from (" +
                                        "select concat(tb_1_.ACCOUNT, ?) c1, concat(tb_1_.EMAIL, ?) c2, " +
                                        "concat(tb_1_.AREA, ?) c3, concat(tb_1_.NICK_NAME, ?) c4 " +
                                        "from SYS_USER tb_1_ where tb_1_.ID = ?" +
                                        ") tb_1_"
                        );
                        it.variables("_m", ".m", "m", "M", 1L);
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into SYS_USER(ACCOUNT, EMAIL, AREA, NICK_NAME) " +
                                        "values(?, ?, ?, ?)"
                        );
                        it.variables("sysusr_001_m", "tom.cook@gmail.com.m", "northm", "TomM");
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.DESCRIPTION " +
                                        "from SYS_USER tb_1_ where tb_1_.ID = ?"
                        );
                        it.variables(100L);
                    });
                    ctx.value(rows -> assertEquals(singletonList("DEFAULT_DESCRIPTION"), rows));
                }
        );
    }

    @Test
    public void testMaterializedInsertWithEmbeddedIdParts() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        ProductTable product = ProductTable.$;
        BaseTable3<StringExpression, StringExpression, StringExpression> source = sqlClient
                .createBaseQuery(product)
                .where(
                        product.id().alpha().eq("00B"),
                        product.id().beta().eq("00A")
                )
                .addSelect(product.id().alpha().concat("X"))
                .addSelect(product.id().beta())
                .addSelect(product.name().concat("+"))
                .asBaseTable();
        jdbc(con -> {
            java.util.List<Tuple3<String, String, String>> rows =
                    sqlClient
                            .createInsert(product, source)
                            .set(product.id().alpha(), source.get_1())
                            .set(product.id().beta(), source.get_2())
                            .set(product.name(), source.get_3())
                            .returning(
                                    product.id().alpha(),
                                    product.id().beta(),
                                    product.name()
                            )
                            .execute(con);
            assertEquals(1, rows.size());
            assertEquals("00BX", rows.get(0).get_1());
            assertEquals("00A", rows.get(0).get_2());
            assertEquals("Bike+", rows.get(0).get_3());
        });
    }

    @Test
    public void testMaterializedUpsertWithEmbeddedIdParts() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        ProductTable product = ProductTable.$;
        BaseTable3<StringExpression, StringExpression, StringExpression> source = sqlClient
                .createBaseQuery(product)
                .where(
                        product.id().alpha().eq("00A"),
                        product.id().beta().eq("00A")
                )
                .addSelect(product.id().alpha())
                .addSelect(product.id().beta())
                .addSelect(product.name().concat("+"))
                .asBaseTable();
        jdbc(con -> {
            java.util.List<Tuple3<String, String, String>> rows =
                    sqlClient
                            .createUpsert(product, source)
                            .key(product.id().alpha(), source.get_1())
                            .key(product.id().beta(), source.get_2())
                            .merge(product.name(), source.get_3())
                            .returning(
                                    product.id().alpha(),
                                    product.id().beta(),
                                    product.name()
                            )
                            .execute(con);
            assertEquals(1, rows.size());
            assertEquals("00A", rows.get(0).get_1());
            assertEquals("00A", rows.get(0).get_2());
            assertEquals("Car+", rows.get(0).get_3());
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void embeddedInsertOnlyPartMustNotUpdate(boolean fallback) {
        JSqlClient client = client(fallback);
        MachineTable table = MachineTable.$;
        jdbc(con -> {
            client.createUpsert(table, singleRowSource(client))
                    .key(table.id(), Expression.value(1L))
                    .insert(table.location().host(), Expression.value("new-host"))
                    .merge(table.location().port(), Expression.value(9090))
                    .execute(con);
            assertEquals(singletonList("localhost"), client.createQuery(table)
                    .where(table.id().eq(1L)).select(table.location().host()).execute(con));
            assertEquals(singletonList(9090), client.createQuery(table)
                    .where(table.id().eq(1L)).select(table.location().port()).execute(con));
        });
    }

    @Test
    public void testMaterializedUpsertRejectsDuplicateKeys() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BookTable book = BookTable.$;
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> row1 = sqlClient
                .createBaseQuery(book)
                .where(book.id().eq(learningGraphQLId1))
                .select(BookUpdateReturningTupleMapper.id(book.id()).name(book.name()));
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> row2 = sqlClient
                .createBaseQuery(book)
                .where(book.id().eq(learningGraphQLId1))
                .select(BookUpdateReturningTupleMapper.id(book.id()).name(book.name()));
        BookUpdateReturningTupleTable source = TypedBaseQuery.unionAll(row1, row2).asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        jdbc(con -> assertThrows(
                IllegalArgumentException.class,
                () -> sqlClient
                        .createUpsert(store, source)
                        .key(store.id(), source.getId())
                        .merge(store.name(), source.getName())
                        .execute(con)
        ));
    }

    @Test
    public void testMaterializedFakeUpsertReturning() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BookStoreTable sourceStore = BookStoreTable.$;
        BookUpdateReturningTupleTable source = sqlClient
                .createBaseQuery(sourceStore)
                .where(sourceStore.id().eq(oreillyId))
                .select(
                        BookUpdateReturningTupleMapper
                                .id(sourceStore.id())
                                .name(sourceStore.name())
                )
                .asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        jdbc(con -> {
            java.util.List<UUID> ids = sqlClient
                    .createUpsert(store, source)
                    .key(store.id(), source.getId())
                    .returning(store.id())
                    .execute(con);
            assertEquals(singletonList(oreillyId), ids);
        });
    }

    @Test
    public void testTransactionTriggerUsesMaterializedPlan() {
        JSqlClient sqlClient = getSqlClient(it -> it.setTriggerType(TriggerType.TRANSACTION_ONLY));
        AtomicReference<EntityEvent<?>> eventRef = new AtomicReference<>();
        sqlClient.getTriggers(true).addEntityListener(
                org.babyfish.jimmer.sql.model.BookStore.class,
                eventRef::set
        );
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = sqlClient
                .createBaseQuery()
                .addSelect(Expression.value(oreillyId))
                .addSelect(Expression.value("O'REILLY-TRIGGER"))
                .asBaseTable();
        BookStoreTable store = BookStoreTable.$;
        jdbc(con -> sqlClient
                .createUpsert(store, source)
                .key(store.id(), source.get_1())
                .merge(store.name(), source.get_2())
                .execute(con));
        EntityEvent<?> event = eventRef.get();
        assertNotNull(event);
        assertNotNull(event.getOldEntity());
        assertNotNull(event.getNewEntity());
    }

    @Test
    public void testInsertFromRootlessBaseQuery() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                getSqlClient()
                        .createBaseQuery()
                        .addSelect(Expression.value(id))
                        .addSelect(Expression.value("ROOTLESS"))
                        .addSelect(Expression.value(0))
                        .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.name(), source.get_2())
                        .set(table.version(), source.get_3()),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_STORE(ID, NAME, VERSION) " +
                                        "select tb_1_.c1, tb_1_.c2, tb_1_.c3 " +
                                        "from (select " +
                                        "cast(? as char(36)) as c1, " +
                                        "cast(? as varchar) as c2, " +
                                        "cast(? as int) as c3" +
                                        ") tb_1_"
                        );
                        it.variables(id, "ROOTLESS", 0);
                    });
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testInsertReturningFromRootlessBaseQuery() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000002");
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                getSqlClient()
                        .createBaseQuery()
                        .addSelect(Expression.value(id))
                        .addSelect(Expression.value("RETURNING"))
                        .addSelect(Expression.value(0))
                        .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.name(), source.get_2())
                        .set(table.version(), source.get_3())
                        .returning(table.id())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "insert into BOOK_STORE(ID, NAME, VERSION) " +
                                    "select tb_1_.c1, tb_1_.c2, tb_1_.c3 " +
                                    "from (select " +
                                    "cast(? as char(36)) as c1, " +
                                    "cast(? as varchar) as c2, " +
                                    "cast(? as int) as c3" +
                                    ") tb_1_" +
                                    ")"
                    ));
                    ctx.value(rows -> assertEquals(singletonList(id), rows));
                }
        );
    }

    @Test
    public void testInsertOnConflictDoNothing() {
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                source(oreillyId, "IGNORED", 0);
        BookStoreTable table = BookStoreTable.$;
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.name(), source.get_2())
                        .set(table.version(), source.get_3())
                        .onConflictDoNothing(table.id()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "merge into BOOK_STORE tb_2_ using " + rootlessSql() + " tb_1_ " +
                                    "on tb_2_.ID = tb_1_.c1 " +
                                    "when not matched then insert(ID, NAME, VERSION) " +
                                    "values(tb_1_.c1, tb_1_.c2, tb_1_.c3)"
                    ));
                    ctx.rowCount(0);
                }
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void insertDoNothingUsesExplicitNaturalKey(boolean fallback) {
        JSqlClient client = client(fallback);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> assertEquals(emptyList(), client.createInsert(table, singleRowSource(client))
                .set(table.name(), Expression.value("O'REILLY"))
                .set(table.id(), Expression.value(UUID.fromString("a0000000-0000-0000-0000-000000000099")))
                .onConflictDoNothing(table.name())
                .returning(table.id())
                .execute(con)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void naturalKeyPreservesInsertIdWhenNoConflict(boolean fallback) {
        JSqlClient client = client(fallback);
        BookStoreTable table = BookStoreTable.$;
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000099");
        jdbc(con -> {
            assertEquals(singletonList(id), client.createUpsert(table, singleRowSource(client))
                    .key(table.name(), Expression.value("NEW-STORE"))
                    .insert(table.id(), Expression.value(id))
                    .merge(table.website(), Expression.value("inserted"))
                    .returning(table.id())
                    .execute(con));
            assertEquals(singletonList("inserted"), client.createQuery(table)
                    .where(table.id().eq(id)).select(table.website()).execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void naturalKeyWithExplicitInsertId(boolean fallback) {
        JSqlClient client = client(fallback);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            assertEquals(singletonList(oreillyId), client.createUpsert(table, singleRowSource(client))
                    .key(table.name(), Expression.value("O'REILLY"))
                    .insert(table.id(), Expression.value(UUID.fromString("a0000000-0000-0000-0000-000000000099")))
                    .merge(table.website(), Expression.value("updated"))
                    .returning(table.id())
                    .execute(con));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void naturalKeyMustNotUpdateAnotherId(boolean fallback) {
        JSqlClient client = client(fallback);
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            assertEquals(singletonList(oreillyId), client.createUpsert(table, singleRowSource(client))
                    .key(table.name(), Expression.value("O'REILLY"))
                    .insert(table.id(), Expression.value(manningId))
                    .merge(table.website(), Expression.value("updated"))
                    .returning(table.id())
                    .execute(con));
            assertEquals(singletonList("updated"), client.createQuery(table)
                    .where(table.id().eq(oreillyId)).select(table.website()).execute(con));
            assertEquals(singletonList(null), client.createQuery(table)
                    .where(table.id().eq(manningId)).select(table.website()).execute(con));
        });
    }

    @Test
    public void testMySqlMaterializedInsertIfAbsentPreservesIdConflictTarget() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000031");
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.setConstraintViolationTranslatable(false);
        });
        BookStoreTable sourceStore = BookStoreTable.$;
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = sqlClient
                .createBaseQuery(sourceStore)
                .where(sourceStore.id().eq(oreillyId))
                .addSelect(Expression.comparable().sql(UUID.class, "cast('" + id + "' as char(36))"))
                .addSelect(sourceStore.name())
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        executeAndExpectRowCount(
                sqlClient
                        .createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.name(), source.get_2())
                        .onConflictDoNothing(table.id()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, tb_1_.c2, ? from (" +
                                    "select cast('" + id + "' as char(36)) c1, tb_1_.NAME c2 " +
                                    "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ where tb_1_.ID = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)"
                    ));
                    ctx.throwable(it -> it.type(ExecutionException.class));
                }
        );
    }

    @Test
    public void testMySqlMaterializedUpsertPreservesIdConflictTarget() {
        UUID id = UUID.fromString("a0000000-0000-0000-0000-000000000032");
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.setConstraintViolationTranslatable(false);
        });
        BookStoreTable sourceStore = BookStoreTable.$;
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = sqlClient
                .createBaseQuery(sourceStore)
                .where(sourceStore.id().eq(oreillyId))
                .addSelect(Expression.comparable().sql(UUID.class, "cast('" + id + "' as char(36))"))
                .addSelect(sourceStore.name())
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .returning(table.id())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, tb_1_.c2, ? from (" +
                                    "select cast('" + id + "' as char(36)) c1, tb_1_.NAME c2 " +
                                    "from BOOK_STORE tb_1_ where tb_1_.ID = ?" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME, tb_1_.VERSION " +
                                    "from BOOK_STORE tb_1_ where tb_1_.ID = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "insert into BOOK_STORE(ID, NAME, VERSION) values(?, ?, ?)"
                    ));
                    ctx.throwable(it -> it.type(ExecutionException.class));
                }
        );
    }

    @Test
    public void testMySqlNativeUpsertForGuaranteedSingleKeyConstraint() {
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    return (R) Integer.valueOf(1);
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
        DepartmentTable table = DepartmentTable.$;
        BaseTable1<StringExpression> source = sqlClient
                .createBaseQuery(table)
                .where(table.id().eq(1L))
                .addSelect(table.name())
                .asBaseTable();
        executeAndExpectRowCount(
                sqlClient
                        .createUpsert(table, source)
                        .key(table.name(), source.get_1()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "insert into DEPARTMENT(NAME, DELETED_MILLIS) " +
                                    "select tb_1_.c1, ? from (" +
                                    "select tb_1_.NAME c1 from DEPARTMENT tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?" +
                                    ") tb_1_ " +
                                    "on duplicate key update DELETED_MILLIS = DELETED_MILLIS"
                    ));
                    ctx.rowCount(1);
                }
        );
    }

    @Test
    public void testMySqlInsertIfAbsentForGuaranteedSingleKeyUsesMaterializedPlan() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new MySqlDialect()));
        DepartmentTable table = DepartmentTable.$;
        BaseTable1<StringExpression> source = sqlClient
                .createBaseQuery(table)
                .where(table.id().eq(1L))
                .addSelect(table.name())
                .asBaseTable();
        executeAndExpectRowCount(
                sqlClient
                        .createInsert(table, source)
                        .set(table.name(), source.get_1())
                        .onConflictDoNothing(table.name()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, ? from (" +
                                    "select tb_1_.NAME c1 from DEPARTMENT tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.ID, tb_1_.NAME " +
                                    "from DEPARTMENT tb_1_ " +
                                    "where tb_1_.NAME = ? and tb_1_.DELETED_MILLIS = ?"
                    ));
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testMySqlMaterializedUpsertDoesNotPreselectGuaranteedSingleKeyConstraint() {
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new MySqlDialect());
            it.setExecutor(new Executor() {
                @Override
                @SuppressWarnings("unchecked")
                public <R> R execute(Args<R> args) {
                    getExecutions().add(Execution.simple(args.sql, args.purpose, args.variables));
                    if (args.sql.startsWith("insert into DEPARTMENT")) {
                        return (R) Integer.valueOf(0);
                    }
                    return DefaultExecutor.INSTANCE.execute(args);
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
        DepartmentTable table = DepartmentTable.$;
        BaseTable1<StringExpression> source = sqlClient
                .createBaseQuery(table)
                .where(table.id().eq(1L))
                .addSelect(table.name())
                .asBaseTable();
        connectAndExpect(
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.name(), source.get_1())
                        .returning(table.name())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, ? from (" +
                                    "select tb_1_.NAME c1 from DEPARTMENT tb_1_ " +
                                    "where tb_1_.ID = ? and tb_1_.DELETED_MILLIS = ?" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "insert into DEPARTMENT(NAME, DELETED_MILLIS) values(?, ?) " +
                                    "on duplicate key update " +
                                    "/* fake update to return all ids */ ID = last_insert_id(ID)"
                    ));
                    ctx.statement(it -> {
                        it.sql(
                                "select tb_1_.ID, tb_1_.NAME from DEPARTMENT tb_1_ " +
                                        "where tb_1_.NAME = ? and tb_1_.DELETED_MILLIS = ?"
                        );
                        it.variables("Market", 0L);
                    });
                    ctx.value(rows -> assertEquals(singletonList("Market"), rows));
                }
        );
    }

    @Test
    public void testInsertOnConflictDoNothingReturningOnlyInsertedRows() {
        UUID insertedId = UUID.fromString("a0000000-0000-0000-0000-000000000013");
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> conflictingRow = getSqlClient()
                .createBaseQuery()
                .select(
                        BookUpdateReturningTupleMapper
                                .id(Expression.value(oreillyId))
                                .name(Expression.value("CONFLICTING"))
                );
        ConfigurableBaseQuery<BookUpdateReturningTupleTable> insertedRow = getSqlClient()
                .createBaseQuery()
                .select(
                        BookUpdateReturningTupleMapper
                                .id(Expression.value(insertedId))
                                .name(Expression.value("INSERTED"))
                );
        BookUpdateReturningTupleTable source = TypedBaseQuery.unionAll(conflictingRow, insertedRow).asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createInsert(table, source)
                        .set(table.id(), source.getId())
                        .set(table.name(), source.getName())
                        .onConflictDoNothing(table.id())
                        .returning(table.name())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select NAME from final table (" +
                                        "merge into BOOK_STORE tb_2_ using (" +
                                        "select cast(? as char(36)) as c1, cast(? as varchar) as c2 " +
                                        "union all " +
                                        "select cast(? as char(36)) as c1, cast(? as varchar) as c2" +
                                        ") tb_1_ on tb_2_.ID = tb_1_.c1 " +
                                        "when not matched then insert(ID, NAME, VERSION) " +
                                        "values(tb_1_.c1, tb_1_.c2, ?)" +
                                        ")"
                        );
                        it.variables(oreillyId, "CONFLICTING", insertedId, "INSERTED", 0);
                    });
                    ctx.value(rows -> assertEquals(singletonList("INSERTED"), rows));
                }
        );
    }

    @Test
    public void testUpsertReturning() {
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                source(oreillyId, "O'REILLY+", 0);
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .insert(table.version(), source.get_3())
                        .returning(table.name())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select NAME from final table (" +
                                    "merge into BOOK_STORE tb_2_ using " + rootlessSql() + " tb_1_ " +
                                    "on tb_2_.ID = tb_1_.c1 " +
                                    "when matched then update set tb_2_.NAME = tb_1_.c2 " +
                                    "when not matched then insert(ID, NAME, VERSION) " +
                                    "values(tb_1_.c1, tb_1_.c2, tb_1_.c3)" +
                                    ")"
                    ));
                    ctx.value(rows -> assertEquals(singletonList("O'REILLY+"), rows));
                }
        );
    }

    @Test
    public void testUpsertReturningTypedTuple() {
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                source(oreillyId, "O'REILLY-TUPLE", 0);
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .insert(table.version(), source.get_3())
                        .returning(
                                BookUpdateReturningTupleMapper
                                        .id(table.id())
                                        .name(table.name())
                        )
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID, NAME from final table (" +
                                    "merge into BOOK_STORE tb_2_ using " + rootlessSql() + " tb_1_ " +
                                    "on tb_2_.ID = tb_1_.c1 " +
                                    "when matched then update set tb_2_.NAME = tb_1_.c2 " +
                                    "when not matched then insert(ID, NAME, VERSION) " +
                                    "values(tb_1_.c1, tb_1_.c2, tb_1_.c3)" +
                                    ")"
                    ));
                    ctx.value((java.util.List<BookUpdateReturningTuple> rows) -> {
                        assertEquals(1, rows.size());
                        assertEquals(oreillyId, rows.get(0).getId());
                        assertEquals("O'REILLY-TUPLE", rows.get(0).getName());
                    });
                }
        );
    }

    @Test
    public void testUpsertWithCustomMergeExpressionAndUpdateWhere() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(oreillyId))
                .addSelect(Expression.value("+"))
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(
                                table.name(),
                                source.get_2(),
                                table.name().concat(source.get_2())
                        )
                        .updateWhere(source.get_2().ne(table.name()))
                        .returning(table.name())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select NAME from final table (" +
                                    "merge into BOOK_STORE tb_2_ using (" +
                                    "select cast(? as char(36)) as c1, cast(? as varchar) as c2" +
                                    ") tb_1_ on tb_2_.ID = tb_1_.c1 " +
                                    "when matched and tb_1_.c2 <> tb_2_.NAME " +
                                    "then update set tb_2_.NAME = concat(tb_2_.NAME, tb_1_.c2) " +
                                    "when not matched then insert(ID, NAME, VERSION) " +
                                    "values(tb_1_.c1, tb_1_.c2, ?)" +
                                    ")"
                    ));
                    ctx.value(rows -> assertEquals(singletonList("O'REILLY+"), rows));
                }
        );
    }

    @Test
    public void testNativeUpdateWhereRejectionReturning() {
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                source(oreillyId, "REJECTED", 0);
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .insert(table.version(), source.get_3())
                        .updateWhere(source.get_2().eq(table.name()))
                        .returning(table.name())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select NAME from final table (" +
                                        "merge into BOOK_STORE tb_2_ using " + rootlessSql() + " tb_1_ " +
                                        "on tb_2_.ID = tb_1_.c1 " +
                                        "when matched and tb_1_.c2 = tb_2_.NAME " +
                                        "then update set tb_2_.NAME = tb_1_.c2 " +
                                        "when not matched then insert(ID, NAME, VERSION) " +
                                        "values(tb_1_.c1, tb_1_.c2, tb_1_.c3)" +
                                        ")"
                        );
                        it.variables(oreillyId, "REJECTED", 0);
                    });
                    ctx.value(rows -> assertEquals(emptyList(), rows));
                }
        );
    }

    @Test
    public void testMaterializedUpsertWithCustomMergeExpressionAndUpdateWhere() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BookStoreTable sourceStore = BookStoreTable.$;
        BookUpdateReturningTupleTable source = sqlClient
                .createBaseQuery(sourceStore)
                .where(sourceStore.id().eq(oreillyId))
                .select(
                        BookUpdateReturningTupleMapper
                                .id(sourceStore.id())
                                .name(sourceStore.name().concat("+"))
                )
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        jdbc(con -> {
            java.util.List<String> rows = sqlClient
                    .createUpsert(table, source)
                    .key(table.id(), source.getId())
                    .merge(
                            table.name(),
                            source.getName(),
                            table.name().concat(source.getName())
                    )
                    .updateWhere(source.getName().ne(table.name()))
                    .returning(table.name())
                    .execute(con);
            assertEquals(singletonList("O'REILLYO'REILLY+"), rows);
        });
    }

    @Test
    public void testValidation() {
        BaseTable2<ComparableExpression<UUID>, StringExpression> source = getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(oreillyId))
                .addSelect(Expression.value("VALUE"))
                .asBaseTable();
        BookStoreTable table = BookStoreTable.$;
        assertThrows(
                IllegalStateException.class,
                () -> getSqlClient()
                        .createInsert(table, source)
                        .set(table.id(), source.get_1())
                        .set(table.id(), source.get_1())
        );
        jdbc(con -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> getSqlClient()
                            .createUpsert(table, source)
                            .insert(table.id(), source.get_1())
                            .execute(con)
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> getSqlClient()
                            .createInsert(table, source)
                            .set(table.id(), source.get_1())
                            .onConflictDoNothing(table.name())
                            .execute(con)
            );
        });
    }

    @Test
    public void testFakeUpsertReturning() {
        BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source =
                source(oreillyId, "UNUSED", 0);
        BookStoreTable table = BookStoreTable.$;
        connectAndExpect(
                con -> getSqlClient()
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .returning(table.id())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select ID from final table (" +
                                    "merge into BOOK_STORE tb_2_ using " + rootlessIdSql() + " tb_1_ " +
                                    "on tb_2_.ID = tb_1_.c1 " +
                                    "when matched then update set tb_2_.VERSION = tb_2_.VERSION " +
                                    "when not matched then insert(ID, VERSION) values(tb_1_.c1, ?)" +
                                    ")"
                    ));
                    ctx.value(rows -> assertEquals(singletonList(oreillyId), rows));
                }
        );
    }

    @Test
    public void testAssociationInsertAndReturning() {
        BaseTable2<ComparableExpression<UUID>, ComparableExpression<UUID>> source =
                getSqlClient()
                        .createBaseQuery()
                        .addSelect(Expression.value(effectiveTypeScriptId1))
                        .addSelect(Expression.value(alexId))
                        .asBaseTable();
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> association =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        connectAndExpect(
                con -> getSqlClient()
                        .createInsert(association, source)
                        .set(association.sourceId(), source.get_1())
                        .set(association.targetId(), source.get_2())
                        .returning(association.sourceId(), association.targetId())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select BOOK_ID, AUTHOR_ID from final table (" +
                                    "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                    "select tb_1_.c1, tb_1_.c2 from (" +
                                    "select cast(? as char(36)) as c1, cast(? as char(36)) as c2" +
                                    ") tb_1_" +
                                    ")"
                    ));
                    ctx.value(rows -> assertEquals(1, rows.size()));
                }
        );
    }

    @Test
    public void testMaterializedAssociationReturning() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(DefaultDialect.INSTANCE));
        BaseTable2<ComparableExpression<UUID>, ComparableExpression<UUID>> source = sqlClient
                .createBaseQuery()
                .addSelect(Expression.comparable().sql(
                        UUID.class,
                        "cast('" + effectiveTypeScriptId1 + "' as char(36))"
                ))
                .addSelect(Expression.comparable().sql(
                        UUID.class,
                        "cast('" + alexId + "' as char(36))"
                ))
                .asBaseTable();
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> association =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        connectAndExpect(
                con -> sqlClient
                        .createInsert(association, source)
                        .set(association.sourceId(), source.get_1())
                        .set(association.targetId(), source.get_2())
                        .returning(association.sourceId(), association.targetId())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, tb_1_.c2 from (" +
                                    "select cast('" + effectiveTypeScriptId1 + "' as char(36)) as c1, " +
                                    "cast('" + alexId + "' as char(36)) as c2" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> {
                        it.sql("insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) values(?, ?)");
                        it.variables(effectiveTypeScriptId1, alexId);
                    });
                    ctx.value(rows -> assertEquals(
                            singletonList(new Tuple2<>(effectiveTypeScriptId1, alexId)),
                            rows
                    ));
                }
        );
    }

    @Test
    public void testAssociationBaseQueryAndInsertOverloads() {
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> sourceAssociation =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        BaseTable2<Expression<UUID>, Expression<UUID>> source =
                getSqlClient()
                        .createBaseQuery(sourceAssociation)
                        .where(
                                sourceAssociation.<UUID>sourceId().eq(learningGraphQLId1),
                                sourceAssociation.<UUID>targetId().eq(alexId)
                        )
                        .addSelect(sourceAssociation.<UUID>sourceId())
                        .addSelect(sourceAssociation.<UUID>targetId())
                        .asBaseTable();
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> targetAssociation =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(targetAssociation, source)
                        .set(targetAssociation.sourceId(), source.get_1())
                        .set(targetAssociation.targetId(), source.get_2())
                        .onConflictDoNothing(),
                ctx -> {
                    ctx.statement(it -> {
                    });
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testAssociationInsertIfAbsent() {
        BaseTable2<ComparableExpression<UUID>, ComparableExpression<UUID>> source =
                getSqlClient()
                        .createBaseQuery()
                        .addSelect(Expression.value(learningGraphQLId1))
                        .addSelect(Expression.value(alexId))
                        .asBaseTable();
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> association =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        executeAndExpectRowCount(
                getSqlClient()
                        .createInsert(association, source)
                        .set(association.sourceId(), source.get_1())
                        .set(association.targetId(), source.get_2())
                        .onConflictDoNothing(),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "merge into BOOK_AUTHOR_MAPPING tb_2_ using (" +
                                    "select cast(? as char(36)) as c1, cast(? as char(36)) as c2" +
                                    ") tb_1_ on tb_2_.BOOK_ID = tb_1_.c1 and tb_2_.AUTHOR_ID = tb_1_.c2 " +
                                    "when not matched then insert(BOOK_ID, AUTHOR_ID) values(tb_1_.c1, tb_1_.c2)"
                    ));
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testMySqlAssociationInsertIfAbsentUsesMaterializedPlan() {
        JSqlClient sqlClient = getSqlClient(it -> it.setDialect(new MySqlDialect()));
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> sourceAssociation =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        BaseTable2<Expression<UUID>, Expression<UUID>> source = sqlClient
                .createBaseQuery(sourceAssociation)
                .where(
                        sourceAssociation.<UUID>sourceId().eq(learningGraphQLId1),
                        sourceAssociation.<UUID>targetId().eq(alexId)
                )
                .addSelect(sourceAssociation.<UUID>sourceId())
                .addSelect(sourceAssociation.<UUID>targetId())
                .asBaseTable();
        AssociationTable<Book, BookTableEx, Author, AuthorTableEx> targetAssociation =
                AssociationTable.of(BookTableEx.class, BookTableEx::authors);
        executeAndExpectRowCount(
                sqlClient
                        .createInsert(targetAssociation, source)
                        .set(targetAssociation.sourceId(), source.get_1())
                        .set(targetAssociation.targetId(), source.get_2())
                        .onConflictDoNothing(),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, tb_1_.c2 from (" +
                                    "select tb_2_.BOOK_ID c1, tb_2_.AUTHOR_ID c2 " +
                                    "from BOOK_AUTHOR_MAPPING tb_2_ " +
                                    "where tb_2_.BOOK_ID = ? and tb_2_.AUTHOR_ID = ?" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "select AUTHOR_ID from BOOK_AUTHOR_MAPPING " +
                                    "where (BOOK_ID, AUTHOR_ID) = (?, ?)"
                    ));
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testH2NullableKeyUpsertUsesMaterializedPlan() {
        testNullableTreeNodeKeyUpsert(getSqlClient(it -> it.setDialect(new H2Dialect())));
    }

    @Test
    public void testMySqlNullableKeyUpsertUsesMaterializedPlan() {
        testNullableTreeNodeKeyUpsert(
                getSqlClient(it -> it
                        .setDialect(new MySqlDialect())
                        .setIdGenerator(TreeNode.class, IdentityIdGenerator.INSTANCE))
        );
    }

    private void testNullableTreeNodeKeyUpsert(JSqlClient sqlClient) {
        TreeNodeTable table = TreeNodeTable.$;
        BaseTable2<StringExpression, NumericExpression<Long>> source = sqlClient
                .createBaseQuery(table)
                .where(table.id().eq(1L))
                .addSelect(table.name())
                .addSelect(table.parentId())
                .asBaseTable();
        executeAndExpectRowCount(
                sqlClient
                        .createUpsert(table, source)
                        .key(table.name(), source.get_1())
                        .key(table.parentId(), source.get_2()),
                ctx -> {
                    ctx.statement(it -> it.sql(
                            "select tb_1_.c1, tb_1_.c2 from (" +
                                    "select tb_1_.NAME c1, tb_1_.PARENT_ID c2 " +
                                    "from TREE_NODE tb_1_ where tb_1_.NODE_ID = ?" +
                                    ") tb_1_"
                    ));
                    ctx.statement(it -> it.sql(
                            "select tb_1_.NODE_ID, tb_1_.NAME, tb_1_.PARENT_ID " +
                                    "from TREE_NODE tb_1_ " +
                                    "where tb_1_.PARENT_ID is null and tb_1_.NAME = ?"
                    ));
                    ctx.statement(it -> it.sql(
                            "update TREE_NODE set " +
                                    "/* fake update to return all ids */ PARENT_ID = PARENT_ID " +
                                    "where NODE_ID = ?"
                    ));
                    ctx.rowCount(1);
                }
        );
    }

    private BaseTable3<ComparableExpression<UUID>, StringExpression, NumericExpression<Integer>> source(
            UUID id,
            String name,
            int version
    ) {
        return getSqlClient()
                .createBaseQuery()
                .addSelect(Expression.value(id))
                .addSelect(Expression.value(name))
                .addSelect(Expression.value(version))
                .asBaseTable();
    }

    private JSqlClient client(boolean fallback) {
        return getSqlClient(it -> it.setDialect(fallback ? DefaultDialect.INSTANCE : new H2Dialect()));
    }

    private BaseTable1<NumericExpression<Integer>> singleRowSource(JSqlClient client) {
        return client.createBaseQuery().addSelect(Expression.constant(1)).asBaseTable();
    }

    private static String rootlessSql() {
        return "(select " +
                "cast(? as char(36)) as c1, " +
                "cast(? as varchar) as c2, " +
                "cast(? as int) as c3" +
                ")";
    }

    private static String rootlessIdSql() {
        return "(select cast(? as char(36)) as c1)";
    }
}
