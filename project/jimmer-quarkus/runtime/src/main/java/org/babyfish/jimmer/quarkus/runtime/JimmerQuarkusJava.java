package org.babyfish.jimmer.quarkus.runtime;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.DataSourceSupport;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class JimmerQuarkusJava implements Jimmer {

    private final DataSources dataSources;

    private final DataSourceSupport dataSourceSupport;

    public JimmerQuarkusJava(DataSources dataSources, DataSourceSupport dataSourceSupport) {
        this.dataSources = dataSources;
        this.dataSourceSupport = dataSourceSupport;
    }

    private final ConcurrentMap<String, JSqlClient> JSqlClients = new ConcurrentHashMap<>();

    public JSqlClient getDefaultJSqlClient() {
        return JSqlClients.computeIfAbsent(DataSourceUtil.DEFAULT_DATASOURCE_NAME, this::doCreateJSqlClient);
    }

    public JSqlClient getJSqlClient(@NotNull String dataSourceName) {
        return JSqlClients.computeIfAbsent(dataSourceName, this::doCreateJSqlClient);
    }

    private JSqlClient doCreateJSqlClient(@NotNull String dataSourceName) {
        AgroalDataSource dataSource = dataSources.getDataSource(dataSourceName);
        Dialect dialect = DBKindEnum.selectDialect(dataSourceSupport.entries.get(dataSourceName).resolvedDbKind);
        return JSqlClient
                .newBuilder()
                .setConnectionManager(new ConnectionManager() {
                    @Override
                    public <R> R execute(Function<Connection, R> block) {
                        try (Connection con = dataSource.getConnection()) {
                            return block.apply(con);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .setDialect(dialect)
                .setExecutor(Executor.log())
                .setSqlFormatter(SqlFormatter.PRETTY)
                .build();
    }
}
