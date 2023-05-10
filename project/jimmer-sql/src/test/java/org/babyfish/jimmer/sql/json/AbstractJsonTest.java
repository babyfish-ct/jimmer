package org.babyfish.jimmer.sql.json;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.JimmerModule;
import org.babyfish.jimmer.sql.model.pg.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractJsonTest {

    private Connection con;

    private JSqlClient sqlClient;

    private List<SQLRecord> records = new ArrayList<>();

    private int recordIndex;

    @BeforeEach
    public void initialize() throws Exception {

        NativeDatabases.assumeNativeDatabase();

        con = NativeDatabases.POSTGRES_DATA_SOURCE.getConnection();
        con.setAutoCommit(false);
        sqlClient = JSqlClient
                .newBuilder()
                .setEntityManager(JimmerModule.ENTITY_MANAGER)
                .setConnectionManager(
                        new ConnectionManager() {
                            @Override
                            public <R> R execute(Function<Connection, R> block) {
                                return block.apply(con);
                            }
                        }
                )
                .setExecutor(
                        new Executor() {
                            @Override
                            public <R> R execute(@NotNull Args<R> args) {
                                records.add(new SQLRecord(args.sql, args.variables));
                                return DefaultExecutor.INSTANCE.execute(args);
                            }
                        }
                )
                .setDialect(new PostgresDialect())
                .addScalarProvider(new PointScalarProvider())
                .addScalarProvider(JsonWrapperProps.TAGS, new TagsScalarProvider())
                .addScalarProvider(JsonWrapperProps.SCORES, new ScoresScalarProvider())
                .build();

        records.clear();
        recordIndex = 0;
    }

    @AfterEach
    public void uninitialize() throws Exception {
        Connection c = con;
        if (c != null) {
            con = null;
            c.rollback();
            c.close();
        }
    }

    protected final JSqlClient sqlClient() {
        if (con == null) {
            throw new IllegalStateException();
        }
        return sqlClient;
    }

    protected void sql(String sql, Object ... variables) {
        SQLRecord record = records.get(recordIndex++);
        Assertions.assertEquals(sql, record.sql);
        Assertions.assertEquals(Arrays.asList(variables), record.variables);
    }

    private static class SQLRecord {

        final String sql;

        final List<Object> variables;

        private SQLRecord(String sql, List<Object> variables) {
            this.sql = sql;
            this.variables = variables;
        }
    }
}
