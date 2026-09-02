package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable2;
import org.babyfish.jimmer.sql.ast.table.base.BaseTable3;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.ld.BoolKeyFile;
import org.babyfish.jimmer.sql.model.ld.BoolKeyFileDraft;
import org.babyfish.jimmer.sql.model.ld.BoolKeyFileTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BooleanLogicalDeletedKeyUpsertTest extends AbstractMutationTest {

    @Test
    public void testUpsertByH2() {
        resetIdentity(null, "BOOL_KEY_FILE");
        List<BoolKeyFile> files = asList(
                BoolKeyFileDraft.$.produce(draft -> {
                    draft.setPath("/active");
                    draft.setName("new-active");
                }),
                BoolKeyFileDraft.$.produce(draft -> {
                    draft.setPath("/deleted");
                    draft.setName("new-deleted");
                })
        );
        executeAndExpectResult(
                getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE))
                        .getEntities()
                        .saveEntitiesCommand(files),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "merge into BOOL_KEY_FILE tb_1_ " +
                                        "using(values(?, ?, ?)) tb_2_(PATH, NAME, DELETED) " +
                                        "--->on tb_1_.PATH = tb_2_.PATH and tb_1_.DELETED = false " +
                                        "when matched then " +
                                        "--->update set NAME = tb_2_.NAME " +
                                        "when not matched then " +
                                        "--->insert(PATH, NAME, DELETED) values(tb_2_.PATH, tb_2_.NAME, tb_2_.DELETED)"
                        );
                        it.batchVariables(0, "/active", "new-active", false);
                        it.batchVariables(1, "/deleted", "new-deleted", false);
                    });
                    ctx.entity(it -> it.modified("{\"id\":1,\"path\":\"/active\",\"name\":\"new-active\"}"));
                    ctx.entity(it -> it.modified("{\"id\":100,\"path\":\"/deleted\",\"name\":\"new-deleted\"}"));
                }
        );
    }

    @Test
    public void testUpsertByPostgres() {
        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE, "BOOL_KEY_FILE");
        List<BoolKeyFile> files = asList(
                BoolKeyFileDraft.$.produce(draft -> {
                    draft.setPath("/active");
                    draft.setName("new-active");
                }),
                BoolKeyFileDraft.$.produce(draft -> {
                    draft.setPath("/deleted");
                    draft.setName("new-deleted");
                })
        );
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                })
                        .getEntities()
                        .saveEntitiesCommand(files),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOL_KEY_FILE as tb_1_(PATH, NAME, DELETED) " +
                                        "values(?, ?, ?) " +
                                        "on conflict(PATH) where DELETED = false " +
                                        "do update set NAME = excluded.NAME " +
                                        "returning ID"
                        );
                        it.batchVariables(0, "/active", "new-active", false);
                        it.batchVariables(1, "/deleted", "new-deleted", false);
                    });
                    ctx.entity(it -> it.modified("{\"id\":1,\"path\":\"/active\",\"name\":\"new-active\"}"));
                    ctx.entity(it -> it.modified("{\"id\":101,\"path\":\"/deleted\",\"name\":\"new-deleted\"}"));
                }
        );
    }

    @Test
    public void testInsertFromSelectUpsertAgainstDeletedRowByH2() {
        resetIdentity(null, "BOOL_KEY_FILE");
        JSqlClient sqlClient = getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE));
        BaseTable2<StringExpression, StringExpression> source = source(sqlClient, "/deleted", "new-deleted");
        BoolKeyFileTable table = BoolKeyFileTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.path(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .returning(table.id(), table.path(), table.name(), table.deleted())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, PATH, NAME, DELETED from final table (" +
                                        "merge into BOOL_KEY_FILE tb_2_ using (" +
                                        "select cast(? as varchar) as c1, cast(? as varchar) as c2" +
                                        ") tb_1_ on tb_2_.PATH = tb_1_.c1 and tb_2_.DELETED = false " +
                                        "when matched then update set tb_2_.NAME = tb_1_.c2 " +
                                        "when not matched then insert(PATH, NAME, DELETED) " +
                                        "values(tb_1_.c1, tb_1_.c2, ?)" +
                                        ")"
                        );
                        it.variables("/deleted", "new-deleted", false);
                    });
                    ctx.value(rows -> {
                        assertEquals(1, rows.size());
                        assertEquals(100L, rows.get(0).get_1());
                        assertEquals("/deleted", rows.get(0).get_2());
                        assertEquals("new-deleted", rows.get(0).get_3());
                        assertFalse(rows.get(0).get_4());
                    });
                }
        );
    }

    @Test
    public void testInsertFromSelectUpsertAgainstActiveRowByH2() {
        resetIdentity(null, "BOOL_KEY_FILE");
        JSqlClient sqlClient = getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE));
        BaseTable2<StringExpression, StringExpression> source = source(sqlClient, "/active", "new-active");
        BoolKeyFileTable table = BoolKeyFileTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.path(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .returning(table.id(), table.path(), table.name(), table.deleted())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, PATH, NAME, DELETED from final table (" +
                                        "merge into BOOL_KEY_FILE tb_2_ using (" +
                                        "select cast(? as varchar) as c1, cast(? as varchar) as c2" +
                                        ") tb_1_ on tb_2_.PATH = tb_1_.c1 and tb_2_.DELETED = false " +
                                        "when matched then update set tb_2_.NAME = tb_1_.c2 " +
                                        "when not matched then insert(PATH, NAME, DELETED) " +
                                        "values(tb_1_.c1, tb_1_.c2, ?)" +
                                        ")"
                        );
                        it.variables("/active", "new-active", false);
                    });
                    ctx.value(rows -> {
                        assertEquals(1, rows.size());
                        assertEquals(1L, rows.get(0).get_1());
                        assertEquals("/active", rows.get(0).get_2());
                        assertEquals("new-active", rows.get(0).get_3());
                        assertFalse(rows.get(0).get_4());
                    });
                }
        );
    }

    @Test
    public void testInsertFromSelectIfAbsentAgainstDeletedRowByH2() {
        resetIdentity(null, "BOOL_KEY_FILE");
        JSqlClient sqlClient = getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE));
        BaseTable2<StringExpression, StringExpression> source = source(sqlClient, "/deleted", "new-deleted");
        BoolKeyFileTable table = BoolKeyFileTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createInsert(table, source)
                        .set(table.path(), source.get_1())
                        .set(table.name(), source.get_2())
                        .onConflictDoNothing(table.path())
                        .returning(table.id(), table.path(), table.name(), table.deleted())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID, PATH, NAME, DELETED from final table (" +
                                        "merge into BOOL_KEY_FILE tb_2_ using (" +
                                        "select cast(? as varchar) as c1, cast(? as varchar) as c2" +
                                        ") tb_1_ on tb_2_.PATH = tb_1_.c1 and tb_2_.DELETED = false " +
                                        "when not matched then insert(PATH, NAME, DELETED) " +
                                        "values(tb_1_.c1, tb_1_.c2, ?)" +
                                        ")"
                        );
                        it.variables("/deleted", "new-deleted", false);
                    });
                    ctx.value(rows -> {
                        assertEquals(1, rows.size());
                        assertEquals(100L, rows.get(0).get_1());
                        assertEquals("/deleted", rows.get(0).get_2());
                        assertEquals("new-deleted", rows.get(0).get_3());
                        assertFalse(rows.get(0).get_4());
                    });
                }
        );
    }

    @Test
    public void testInsertFromSelectIdConflictIsNotRestrictedByLogicalDelete() {
        JSqlClient sqlClient = getSqlClient(it -> it.setIdGenerator(IdentityIdGenerator.INSTANCE));
        BaseTable3<NumericExpression<Long>, StringExpression, StringExpression> source = sqlClient
                .createBaseQuery()
                .addSelect(Expression.value(1L))
                .addSelect(Expression.value("/unused"))
                .addSelect(Expression.value("new-active"))
                .asBaseTable();
        BoolKeyFileTable table = BoolKeyFileTable.$;
        connectAndExpect(
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.id(), source.get_1())
                        .insert(table.path(), source.get_2())
                        .merge(table.name(), source.get_3())
                        .returning(table.id())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select ID from final table (" +
                                        "merge into BOOL_KEY_FILE tb_2_ using (" +
                                        "select cast(? as bigint) as c1, " +
                                        "cast(? as varchar) as c2, cast(? as varchar) as c3" +
                                        ") tb_1_ on tb_2_.ID = tb_1_.c1 " +
                                        "when matched then update set tb_2_.NAME = tb_1_.c3 " +
                                        "when not matched then insert(ID, PATH, NAME, DELETED) " +
                                        "values(tb_1_.c1, tb_1_.c2, tb_1_.c3, ?)" +
                                        ")"
                        );
                        it.variables(1L, "/unused", "new-active", false);
                    });
                    ctx.value(rows -> assertEquals(singletonList(1L), rows));
                }
        );
    }

    @Test
    public void testInsertFromSelectUpsertAgainstDeletedRowByPostgres() {
        NativeDatabases.assumeNativeDatabase();
        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE, "BOOL_KEY_FILE");
        JSqlClient sqlClient = getSqlClient(it -> {
            it.setDialect(new PostgresDialect());
            it.setIdGenerator(IdentityIdGenerator.INSTANCE);
        });
        BaseTable2<StringExpression, StringExpression> source = source(sqlClient, "/deleted", "new-deleted");
        BoolKeyFileTable table = BoolKeyFileTable.$;
        connectAndExpect(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                con -> sqlClient
                        .createUpsert(table, source)
                        .key(table.path(), source.get_1())
                        .merge(table.name(), source.get_2())
                        .returning(table.id(), table.path(), table.name(), table.deleted())
                        .execute(con),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOL_KEY_FILE as tb_2_(PATH, NAME, DELETED) " +
                                        "select tb_1_.c1, tb_1_.c2, ? from (" +
                                        "select ? as c1, ? as c2" +
                                        ") tb_1_ " +
                                        "on conflict(PATH) where DELETED = false " +
                                        "do update set NAME = excluded.NAME " +
                                        "returning ID, PATH, NAME, DELETED"
                        );
                        it.variables(false, "/deleted", "new-deleted");
                    });
                    ctx.value(rows -> {
                        assertEquals(1, rows.size());
                        assertEquals(100L, rows.get(0).get_1());
                        assertEquals("/deleted", rows.get(0).get_2());
                        assertEquals("new-deleted", rows.get(0).get_3());
                        assertFalse(rows.get(0).get_4());
                    });
                }
        );
    }

    private BaseTable2<StringExpression, StringExpression> source(
            JSqlClient sqlClient,
            String path,
            String name
    ) {
        return sqlClient
                .createBaseQuery()
                .addSelect(Expression.value(path))
                .addSelect(Expression.value(name))
                .asBaseTable();
    }
}
