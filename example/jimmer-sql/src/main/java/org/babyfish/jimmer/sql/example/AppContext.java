package org.babyfish.jimmer.sql.example;

import com.zaxxer.hikari.HikariDataSource;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.example.model.Gender;
import org.babyfish.jimmer.sql.runtime.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class AppContext {

    private AppContext() {}

    private static final HikariDataSource DATA_SOURCE;

    public static final SqlClient SQL_CLIENT;

    public static void close() {
        DATA_SOURCE.close();
    }

    static {

        DATA_SOURCE = new HikariDataSource();
        DATA_SOURCE.setDriverClassName("org.h2.Driver");
        DATA_SOURCE.setJdbcUrl("jdbc:h2:mem:jimmer-sql");

        SQL_CLIENT = SqlClient.newBuilder()
                .setConnectionManager(
                        ConnectionManager.simpleConnectionManager(DATA_SOURCE)
                )
                .setDialect(DefaultDialect.INSTANCE)
                .setExecutor(
                        new Executor() {

                            @Override
                            public <R> R execute(
                                    Connection con,
                                    String sql,
                                    List<Object> variables,
                                    SqlFunction<PreparedStatement, R> block
                            ) {
                                System.err.println("jdbc sql: " + sql);
                                System.err.println("jdbc parameters: " + variables);
                                return DefaultExecutor.INSTANCE.execute(con, sql, variables, block);
                            }
                        }
                )
                .addScalarProvider(
                        ScalarProvider.enumProviderByString(Gender.class, it -> {
                            it.map(Gender.MALE, "M");
                            it.map(Gender.FEMALE, "F");
                        })
                )
                .build();

        InputStream stream = AppContext.class
                .getClassLoader()
                .getResourceAsStream("database.sql");
        if (stream == null) {
            throw new RuntimeException("Cannot load database.sql");
        }

        try (Reader reader = new InputStreamReader(stream)) {
            StringBuilder builder = new StringBuilder();
            char[] buf = new char[1024];
            while (true) {
                int len = reader.read(buf);
                if (len == -1) {
                    break;
                }
                builder.append(buf, 0, len);
            }
            SQL_CLIENT.getConnectionManager().execute(con -> {
                try {
                    return con.createStatement().executeUpdate(builder.toString());
                } catch (SQLException ex) {
                    throw new RuntimeException("Cannot init database", ex);
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read database initialization sql", ex);
        }
    }
}
