package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.ld.BoolKeyFile;
import org.babyfish.jimmer.sql.model.ld.BoolKeyFileDraft;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class BooleanLogicalDeletedKeyUpsertTest extends AbstractMutationTest {

    @Test
    public void testUpsertByH2() {
        resetIdentity(null, "BOOL_KEY_FILE");
        List<BoolKeyFile> files = Arrays.asList(
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
        List<BoolKeyFile> files = Arrays.asList(
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
                                "insert into BOOL_KEY_FILE(PATH, NAME, DELETED) " +
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
}
