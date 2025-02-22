package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator;
import org.babyfish.jimmer.sql.model.Immutables;
import org.babyfish.jimmer.sql.model.pg.MacAddressScalarProvider;
import org.babyfish.jimmer.sql.model.pg.PgTypeRow;
import org.babyfish.jimmer.sql.model.pg.PgTypeRowProps;
import org.junit.jupiter.api.Test;

public class PgTypeRowTest extends AbstractMutationTest {

    @Test
    public void insert() {

        NativeDatabases.assumeNativeDatabase();

        PgTypeRow row = Immutables.createPgTypeRow(draft -> {
            draft.setMacAddress("08:00:2b:01:02:03");
        });

        resetIdentity(NativeDatabases.POSTGRES_DATA_SOURCE, "pg_type_row");
        executeAndExpectResult(
                NativeDatabases.POSTGRES_DATA_SOURCE,
                getSqlClient(it -> {
                    it.setDialect(new PostgresDialect());
                    it.setIdGenerator(IdentityIdGenerator.INSTANCE);
                    it.setScalarProvider(PgTypeRowProps.MAC_ADDRESS, new MacAddressScalarProvider());
                }).saveCommand(row).setMode(SaveMode.INSERT_ONLY),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql("insert into PG_TYPE_ROW(MAC_ADDRESS) values(?) returning ID");
                    });
                    ctx.entity(it -> {
                        it.modified(
                                "{\"id\":100,\"macAddress\":\"08:00:2b:01:02:03\"}"
                        );
                    });
                }
        );
    }
}
