package org.babyfish.jimmer.sql.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.NativeDatabases;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.model.pg.*;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
                .setConnectionManager(ConnectionManager.singleConnectionManager(con))
                .setExecutor(
                        new Executor() {
                            @Override
                            public <R> R execute(@NotNull Args<R> args) {
                                records.add(new SQLRecord(args.sql, args.variables));
                                return DefaultExecutor.INSTANCE.execute(args);
                            }

                            @Override
                            public BatchContext executeBatch(
                                    @NotNull Connection con,
                                    @NotNull String sql,
                                    @Nullable ImmutableProp generatedIdProp,
                                    @NotNull ExecutionPurpose purpose,
                                    @NotNull JSqlClientImplementor sqlClient
                            ) {
                                SQLRecord sqlRecord = new SQLRecord(sql);
                                records.add(sqlRecord);
                                BatchContext ctx = DefaultExecutor.INSTANCE.executeBatch(
                                        con,
                                        sql,
                                        generatedIdProp,
                                        purpose,
                                        sqlClient
                                );
                                return new BatchContext() {

                                    @Override
                                    public String sql() {
                                        return ctx.sql();
                                    }

                                    @Override
                                    public JSqlClientImplementor sqlClient() {
                                        return ctx.sqlClient();
                                    }

                                    @Override
                                    public ExecutionPurpose purpose() {
                                        return ctx.purpose();
                                    }

                                    @Override
                                    public ExecutorContext executorContext() {
                                        return ctx.executorContext();
                                    }

                                    @Override
                                    public void add(List<Object> variables) {
                                        sqlRecord.add(variables);
                                        ctx.add(variables);
                                    }

                                    @Override
                                    public int[] execute(Function<SQLException, Exception> exceptionTranslator) {
                                        return ctx.execute(exceptionTranslator);
                                    }

                                    @Override
                                    public Object[] generatedIds() {
                                        return ctx.generatedIds();
                                    }

                                    @Override
                                    public void close() {
                                        ctx.close();
                                    }
                                };
                            }
                        }
                )
                .setDialect(new PostgresDialect())
                .setScalarProvider(JsonWrapperProps.TAGS, new TagsScalarProvider())
                .setScalarProvider(JsonWrapperProps.SCORES, new ScoresScalarProvider())
                .setDefaultSerializedTypeObjectMapper(new ObjectMapper())
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
        Assertions.assertEquals(Arrays.asList(variables), record.variableLists.get(0));
    }

    protected void batchSql(String sql, List<Object> ... variableLists) {
        SQLRecord record = records.get(recordIndex++);
        Assertions.assertEquals(sql, record.sql);
        Assertions.assertEquals(variableLists.length, record.variableLists.size());
        for (int i = 0; i < variableLists.length; i++) {
            Assertions.assertEquals(variableLists[i], record.variableLists.get(i));
        }
    }

    private static class SQLRecord {

        final String sql;

        final List<List<Object>> variableLists;

        SQLRecord(String sql, List<Object> variables) {
            this.sql = sql;
            this.variableLists = Collections.singletonList(variables);
        }

        SQLRecord(String sql) {
            this.sql = sql;
            this.variableLists = new ArrayList<>();
        }

        void add(List<Object> variables) {
            this.variableLists.add(variables);
        }
    }
}
