package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.TransientResolver;
import org.babyfish.jimmer.sql.ast.mutation.QueryReason;
import org.babyfish.jimmer.sql.di.DefaultTransientResolverProvider;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.impl.mutation.Mutations;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.loader.graphql.Loaders;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.model.calc.BookStoreMostPopularAuthorResolver;
import org.babyfish.jimmer.sql.runtime.*;
import org.h2.Driver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.*;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AbstractTest extends Tests {

    protected static final String JDBC_URL = "jdbc:h2:./build/h2/jimmer_test_db;database_to_upper=true;time zone=GMT+8";

    protected static final Object UNKNOWN_VARIABLE = new Object() {
        @Override
        public String toString() {
            return "<unknown-object>";
        }
    };

    private final Map<Class<?>, AutoIds> autoIdMap = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
        jdbc(AbstractTest::initDatabase);
    }

    @BeforeEach
    public void beforeTest() {
        executions.clear();
    }

    private final JSqlClient sqlClient = getSqlClient(it -> {
        UserIdGenerator<?> idGenerator = this::autoId;
        it.setIdGenerator(idGenerator);
    });

    private final LambdaClient lambdaClient = new LambdaClient(getSqlClient());

    private final List<Execution> executions = new ArrayList<>();

    private class ExecutorImpl implements Executor {

        @Override
        public <R> R execute(@NotNull Args<R> args) {
            executions.add(Execution.simple(args.sql, args.purpose, args.variables));
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
            return new BatchContextImpl(
                    DefaultExecutor.INSTANCE.executeBatch(
                            con,
                            sql,
                            generatedIdProp,
                            purpose,
                            sqlClient
                    )
            );
        }
    }

    protected JSqlClient getSqlClient() {
        return sqlClient;
    }

    protected JSqlClient getSqlClient(Consumer<JSqlClient.Builder> block) {
        JSqlClient.Builder builder = JSqlClient.newBuilder()
                .setExecutor(new ExecutorImpl())
                .setDialect(new H2Dialect() {
                    @Override
                    public boolean isAnyEqualityOfArraySupported() {
                        return AbstractTest.this.isAnyEqualityOfArraySupported();
                    }
                })
                .setTransientResolverProvider(
                        new DefaultTransientResolverProvider() {
                            @Override
                            public TransientResolver<?, ?> get(
                                    String ref,
                                    JSqlClient sqlClient
                            ) throws Exception {
                                if (ref.equals("bookStoreMostPopularAuthorResolver")) {
                                    return new BookStoreMostPopularAuthorResolver(sqlClient);
                                }
                                return super.get(ref, sqlClient);
                            }
                        }
                );
        if (block != null) {
            block.accept(builder);
        }
        return builder.build();
    }

    protected LambdaClient getLambdaClient() {
        return lambdaClient;
    }

    protected LambdaClient getLambdaClient(Consumer<JSqlClient.Builder> block) {
        return new LambdaClient(getSqlClient(block));
    }

    protected List<Execution> getExecutions() {
        return executions;
    }

    protected void clearExecutions() {
        executions.clear();
    }

    protected boolean isAnyEqualityOfArraySupported() {
        return false;
    }

    protected static class Execution {

        private final String sql;

        private final ExecutionPurpose purpose;

        private final List<List<Object>> variablesList;

        private Execution(String sql, ExecutionPurpose purpose, List<List<Object>> variablesList) {
            this.sql = sql;
            this.purpose = purpose;
            this.variablesList = variablesList;
        }

        public static Execution simple(String sql, ExecutionPurpose purpose, List<Object> variables) {
            return new Execution(sql, purpose, Collections.singletonList(variables));
        }

        public static Execution batch(String sql, List<List<Object>> variablesList) {
            return new Execution(sql, ExecutionPurpose.command(QueryReason.NONE), variablesList);
        }

        public String getSql() {
            return sql;
        }

        public ExecutionPurpose getPurpose() {
            return purpose;
        }

        public int getBatchCount() {
            return variablesList.size();
        }

        public List<Object> getVariables(int batchIndex) {
            return variablesList.get(batchIndex);
        }
    }

    public static ConnectionManager testConnectionManager() {
        return new TestConnectionManager();
    }

    public static void jdbc(SqlConsumer<Connection> block) {
        jdbc(null, false, block);
    }

    public static void jdbc(DataSource dataSource, boolean rollback, SqlConsumer<Connection> block) {
        try (Connection con = dataSource != null ?
                dataSource.getConnection() :
                new Driver().connect(JDBC_URL, null)) {
            if (rollback) {
                con.setAutoCommit(false);
                try {
                    block.accept(con);
                } finally {
                    con.rollback();
                }
            } else {
                block.accept(con);
            }
        } catch (SQLException ex) {
            Assertions.fail("SQL error", ex);
        }
    }

    public interface SqlConsumer<T> {
        void accept(T value) throws SQLException;
    }

    protected static void initDatabase(Connection con) {

        InputStream stream = AbstractTest.class.getClassLoader().getResourceAsStream("database.sql");
        if (stream == null) {
            Assertions.fail("Failed to initialize database, cannot load 'database.sql'");
        }

        try (Reader reader = new InputStreamReader(stream)) {
            StringBuilder builder = new StringBuilder();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
            con.createStatement().execute(builder.toString());
        } catch (IOException | SQLException ex) {
            Assertions.fail("Failed to initialize database", ex);
        }
    }

    protected static void initDatabase(Connection conn, String name) {
        final InputStream stream = AbstractTest.class.getClassLoader().getResourceAsStream(name);
        if (stream == null) {
            throw new IllegalStateException("Cannot load '" + name + "'");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            StringBuilder sql = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                sql.append(line).append(" ");
                if (line.endsWith(";")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(sql.toString());
                        sql.setLength(0);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    protected void setAutoIds(Class<?> entityType, Object ... args) {
        autoIdMap.put(entityType, new AutoIds(Arrays.asList(args.clone())));
    }

    protected Object autoId(Class<?> entityType) {
        AutoIds autoIds = autoIdMap.get(entityType);
        if (autoIds == null) {
            throw new IllegalStateException("No prepared auto ids for \"" + entityType.getName() + "\"");
        }
        return autoIds.get();
    }

    private static class AutoIds {

        private final List<Object> ids;

        private int index;

        public AutoIds(List<Object> ids) {
            this.ids = ids;
        }

        public Object get() {
            return ids.get(index++);
        }
    }

    protected static class LambdaClient {

        private final JSqlClientImplementor sqlClient;

        public LambdaClient(JSqlClient sqlClient) {
            this.sqlClient = (JSqlClientImplementor) sqlClient;
        }

        public Entities getEntities() {
            return sqlClient.getEntities();
        }

        public Triggers getTriggers() {
            return sqlClient.getTriggers();
        }

        public Loaders getLoaders() {
            return sqlClient.getLoaders();
        }

        public <T extends Table<?>, R> ConfigurableRootQuery<T, R> createQuery(
                Class<T> tableType,
                BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, R>> block
        ) {
            return Queries.createQuery(sqlClient, tableType, block);
        }

        public <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
        ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
                Class<ST> sourceTableType,
                Function<ST, TT> targetTableGetter,
                BiFunction<
                    MutableRootQuery<AssociationTable<SE, ST, TE, TT>>,
                    AssociationTable<SE, ST, TE, TT>,
                    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R>
                > block
        ) {
            return Queries.createAssociationQuery(sqlClient, sourceTableType, targetTableGetter, block);
        }

        public <T extends Table<?>> Executable<Integer> createUpdate(
                Class<T> tableType,
                BiConsumer<MutableUpdate, T> block
        ) {
            return Mutations.createUpdate(sqlClient, tableType, block);
        }

        public <T extends Table<?>> Executable<Integer> createDelete(
                Class<T> tableType,
                BiConsumer<MutableDelete, T> block
        ) {
            return Mutations.createDelete(sqlClient, tableType, block);
        }

        public <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
                Filterable parent,
                Class<T> tableType,
                BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
        ) {
            return Queries.createSubQuery(parent, tableType, block);
        }

        public <T extends Table<?>> MutableSubQuery createWildSubQuery(
                Filterable parent,
                Class<T> tableType,
                BiConsumer<MutableSubQuery, T> block
        ) {
            return Queries.createWildSubQuery(parent, tableType, block);
        }

        public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
        ConfigurableSubQuery<R> createAssociationSubQuery(
                Filterable parent,
                Class<ST> sourceTableType,
                Function<ST, TT> targetTableGetter,
                BiFunction<MutableSubQuery, AssociationTable<SE, ST, TE, TT>, ConfigurableSubQuery<R>> block
        ) {
            return Queries.createAssociationSubQuery(parent, sourceTableType, targetTableGetter, block);
        }

        public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
        MutableSubQuery createAssociationWildSubQuery(
                Filterable parent,
                Class<ST> sourceTableType,
                Function<ST, TT> targetTableGetter,
                BiConsumer<MutableSubQuery, AssociationTable<SE, ST, TE, TT>> block
        ) {
            return Queries.createAssociationWildSubQuery(parent, sourceTableType, targetTableGetter, block);
        }
    }

    public static class TestConnectionManager implements ConnectionManager {
        @Override
        @SuppressWarnings("unchecked")
        public <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
            R[] resultBox = (R[]) new Object[1];
            if (con == null) {
                jdbc(c -> resultBox[0] = block.apply(c));
            } else {
                resultBox[0] = block.apply(con);
            }
            return resultBox[0];
        }
    }

    protected class BatchContextImpl implements Executor.BatchContext {

        private final Executor.BatchContext raw;

        private List<List<Object>> variablesList = new ArrayList<>();

        public BatchContextImpl(Executor.BatchContext raw) {
            this.raw = raw;
        }

        @Override
        public String sql() {
            return raw.sql();
        }

        @Override
        public JSqlClientImplementor sqlClient() {
            return raw.sqlClient();
        }

        @Override
        public ExecutionPurpose purpose() {
            return raw.purpose();
        }

        @Override
        public ExecutorContext ctx() {
            return raw.ctx();
        }

        @Override
        public void add(List<Object> variables) {
            raw.add(variables);
            variablesList.add(variables);
        }

        @Override
        public int[] execute(BiFunction<SQLException, ExceptionTranslator.Args, Exception> exceptionTranslator) {
            executions.add(AbstractTest.Execution.batch(raw.sql(), variablesList));
            return raw.execute(exceptionTranslator);
        }

        @Override
        public Object[] generatedIds() {
            return raw.generatedIds();
        }

        @Override
        public void addExecutedListener(Runnable listener) {
            raw.addExecutedListener(listener);
        }

        @Override
        public void close() {
            raw.close();
        }
    }
}
