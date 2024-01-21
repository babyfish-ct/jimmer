package org.babyfish.jimmer.quarkus.runtime;

import io.quarkus.agroal.runtime.DataSourceSupport;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Supplier;

@Recorder
public class JimmerDataSourcesRecord {

    private static final Logger log = Logger.getLogger(JimmerDataSourcesRecord.class.getName());

    public Supplier<JimmerQuarkusJava> setupJSqlClientDataSourcesSupplier() {
        ArcContainer container = Arc.container();
        if (null == container) {
            throw new RuntimeException("Unable to initialize the ArcContainer");
        } else {
            if (container.instance(DataSources.class).isAvailable()) {
                return () -> new JimmerQuarkusJava(container.instance(DataSources.class).get(),
                        container.instance(DataSourceSupport.class).get());
            } else {
                log.debug("Jimmer No data source configured");
                return null;
            }
        }
    }

    public Function<SyntheticCreationalContext<JSqlClient>, JSqlClient> JSqlClientFunction(String dataSourceName, String dbKind) {
        ArcContainer container = Arc.container();
        DataSources dataSources = container.instance(DataSources.class).get();
        return context -> JSqlClient
                .newBuilder()
                .setConnectionManager(new ConnectionManager() {
                    @Override
                    public <R> R execute(Function<Connection, R> block) {
                        try (Connection con = dataSources.getDataSource(dataSourceName).getConnection()) {
                            return block.apply(con);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .setDialect(DBKindEnum.selectDialect(dbKind))
                .setExecutor(Executor.log())
                .setSqlFormatter(SqlFormatter.PRETTY)
                .build();
    }
}
