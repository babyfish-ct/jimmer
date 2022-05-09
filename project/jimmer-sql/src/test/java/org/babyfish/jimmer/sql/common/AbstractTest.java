package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.model.Gender;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlFunction;
import org.h2.Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AbstractTest {

    private static final String JDBC_URL = "jdbc:h2:~/jdbc_test_db";

    private DynamicDialect dynamicDialect = new DynamicDialect();

    @BeforeEach
    public void beforeTest() {
        executions.clear();
    }

    private SqlClient sqlClient = SqlClient
            .newBuilder()
            .setDialect(dynamicDialect)
            .setExecutor(new ExecutorImpl())
            .addScalarProvider(
                    ScalarProvider.enumProviderByString(Gender.class, builder -> {
                        builder
                                .map(Gender.MALE, "M")
                                .map(Gender.FEMALE, "F");
                    })
            )
            .build();

    protected void using(Dialect dialect, Runnable block) {
        Dialect oldTargetDialect = dynamicDialect.targetDialect;
        dynamicDialect.targetDialect = dialect;
        try {
            block.run();
        } finally {
            dynamicDialect.targetDialect = oldTargetDialect;
        }
    }

    private List<Execution> executions = new ArrayList<>();

    private class ExecutorImpl implements Executor {

        @Override
        public <R> R execute(
                Connection con,
                String sql,
                List<Object> variables,
                SqlFunction<PreparedStatement, R> block
        ) {
            executions.add(new Execution(sql, variables));
            return DefaultExecutor.INSTANCE.execute(con, sql, variables, block);
        }
    }

    protected SqlClient getSqlClient() {
        return sqlClient;
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

    protected static void jdbc(SqlConsumer<Connection> consumer) {
        try (Connection con = new Driver().connect(JDBC_URL, null)) {
            consumer.accept(con);
        } catch (SQLException ex) {
            Assertions.fail("SQL error", ex);
        }
    }

    protected interface SqlConsumer<T> {
        void accept(T value);
    }

    protected static void initDatabase(Connection con) {

        InputStream stream = AbstractTest.class.getClassLoader().getResourceAsStream("database.sql");
        if (stream == null) {
            Assertions.fail("Failed to initialize database, cannot load 'database.sql'");
        }

        StringBuilder builder = new StringBuilder();
        try (Reader reader = new InputStreamReader(stream)) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                builder.append(buf, 0, len);
            }
        } catch (IOException ex) {
            Assertions.fail("Failed to initialize database", ex);
        }

        for (String part : builder.toString().split(";")) {
            String sql = part.trim();
            if (!sql.isEmpty()) {
                try {
                    con.createStatement().executeUpdate(sql);
                } catch (SQLException ex) {
                    Assertions.fail("Failed to initialize database", ex);
                }
            }
        }
    }
}
