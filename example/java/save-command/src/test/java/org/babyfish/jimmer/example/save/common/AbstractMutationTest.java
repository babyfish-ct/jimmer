package org.babyfish.jimmer.example.save.common;

import org.babyfish.jimmer.example.save.model.JimmerModule;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.runtime.*;
import org.h2.Driver;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Properties;
import java.util.function.Function;

public abstract class AbstractMutationTest {

    private Connection connection;

    private JSqlClient sqlClient;

    private List<ExecutedStatement> executedStatements;

    @BeforeEach
    public void beforeTest() throws SQLException {
        connection = createConnection();
        createDatabase(connection);
        executedStatements = new ArrayList<>();
        JSqlClient.Builder builder = JSqlClient
                .newBuilder()
                .setEntityManager(JimmerModule.ENTITY_MANAGER)
                .setDialect(new H2Dialect())
                .setExecutor(new RecordSqlExecutor())
                .setConnectionManager(new ExistsConnectionManager());
        customize(builder);
        sqlClient = builder.build();
    }

    @AfterEach
    public void afterTest() throws SQLException {
        sqlClient = null;
        Connection con = connection;
        if (con != null) {
            connection = null;
            con.close();
        }
    }

    protected JSqlClient sql() {
        return sqlClient;
    }

    protected void jdbc(String sql, Object ... args) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void assertExecutedStatements(ExecutedStatement... executedStatements) {
        int count = Math.min(this.executedStatements.size(), executedStatements.length);
        for (int i = 0; i < count; i++) {
            Assertions.assertEquals(
                    executedStatements[i].getSql(),
                    this.executedStatements.get(i).getSql(),
                    "Failed to assert sql of statements[" + i + "]"
            );
            Assertions.assertEquals(
                    executedStatements[i].getVariables(),
                    this.executedStatements.get(i).getVariables(),
                    "Failed to assert variables of statements[" + i + "]"
            );
        }
        Assertions.assertEquals(
                executedStatements.length,
                this.executedStatements.size(),
                "Expected " +
                        executedStatements.length +
                        " statements, but " +
                        this.executedStatements.size() +
                        " statements"
        );
    }

    // Can be overridden
    protected void customize(JSqlClient.Builder builder) {}

    private static Connection createConnection() throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("database_to_upper", "true");
        return new Driver().connect(
                "jdbc:h2:mem:save-cmd-example",
                properties
        );
    }

    private void createDatabase(Connection con) {
        InputStream stream = AbstractMutationTest.class.getClassLoader().getResourceAsStream("unit-test.sql");
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

    private class RecordSqlExecutor implements Executor {

        @Override
        public <R> R execute(
                Connection con,
                String sql,
                List<Object> variables,
                ExecutionPurpose purpose,
                StatementFactory statementFactory,
                SqlFunction<PreparedStatement, R> block
        ) {
            executedStatements.add(
                    new ExecutedStatement(
                            sql,
                            variables.toArray(new Object[0])
                    )
            );
            return DefaultExecutor.INSTANCE.execute(
                    con,
                    sql,
                    variables,
                    purpose,
                    statementFactory,
                    block
            );
        }
    }

    private class ExistsConnectionManager implements ConnectionManager {

        @Override
        public <R> R execute(Function<Connection, R> block) {
            return block.apply(connection);
        }
    }
}
