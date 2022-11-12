package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.Entities;
import org.babyfish.jimmer.sql.JSqlClient;
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
import org.babyfish.jimmer.sql.loader.Loaders;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.babyfish.jimmer.sql.runtime.*;
import org.h2.Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AbstractTest {

    private static final String JDBC_URL = "jdbc:h2:~/jimmer_test_db;database_to_upper=true";

    private Map<Class<?>, AutoIds> autoIdMap = new HashMap<>();

    @BeforeAll
    public static void beforeAll() {
        jdbc(AbstractTest::initDatabase);
    }

    @BeforeEach
    public void beforeTest() {
        executions.clear();
    }

    private JSqlClient sqlClient = getSqlClient(it -> {
        UserIdGenerator idGenerator = this::autoId;
        it.setIdGenerator(idGenerator);
    });

    private LambdaClient lambdaClient = new LambdaClient(getSqlClient());

    private List<Execution> executions = new ArrayList<>();

    private class ExecutorImpl implements Executor {

        @Override
        public <R> R execute(
                Connection con,
                String sql,
                List<Object> variables,
                ExecutionPurpose purpose,
                StatementFactory statementFactory,
                SqlFunction<PreparedStatement, R> block
        ) {
            executions.add(new Execution(sql, variables));
            return DefaultExecutor.INSTANCE.execute(con, sql, variables, purpose, statementFactory, block);
        }
    }

    protected JSqlClient getSqlClient() {
        return sqlClient;
    }

    protected JSqlClient getSqlClient(Consumer<JSqlClient.Builder> block) {
        JSqlClient.Builder builder = JSqlClient.newBuilder()
                .setExecutor(new ExecutorImpl());
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

    protected static class Execution {

        private String sql;

        private List<Object> variables;

        Execution(String sql, List<Object> variables) {
            this.sql = sql;
            this.variables = variables;
        }

        public String getSql() {
            return sql;
        }

        public List<Object> getVariables() {
            return variables;
        }
    }

    protected static void jdbc(SqlConsumer<Connection> block) {
        jdbc(null, false, block);
    }

    protected static void jdbc(DataSource dataSource, boolean rollback, SqlConsumer<Connection> block) {
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

    protected interface SqlConsumer<T> {
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

        private List<Object> ids;

        private int index;

        public AutoIds(List<Object> ids) {
            this.ids = ids;
        }

        public Object get() {
            return ids.get(index++);
        }
    }

    protected static class LambdaClient {

        private final JSqlClient sqlClient;

        public LambdaClient(JSqlClient sqlClient) {
            this.sqlClient = sqlClient;
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
}
