package org.babyfish.jimmer.sql.formatter;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.DefaultExecutor;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;
import org.h2.Driver;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractFormatterTest extends AbstractTest {

    private Connection con;

    private JSqlClient sqlClient;

    private List<String> sqlList = new ArrayList<>();

    @BeforeEach
    public void initialize() throws Exception {

        con = new Driver().connect(JDBC_URL, null);
        con.setAutoCommit(false);
        sqlClient = JSqlClient
                .newBuilder()
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
                                sqlList.add(args.sql);
                                return DefaultExecutor.INSTANCE.execute(args);
                            }
                        }
                )
                .setSqlFormatter(new SqlFormatter(true, "    ", 3, 100))
                .build();

        sqlList.clear();
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

    protected void assertSqlStatements(String ... sqlStatements) {
        Assertions.assertEquals(
                Arrays.asList(sqlStatements),
                sqlList
        );
    }
}
