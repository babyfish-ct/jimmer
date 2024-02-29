package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.spring.cfg.support.DataSourceAwareConnectionManager;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * This transaction manager is only required
 * when {@link JTransactionalSqlClient} is used
 */
public class JimmerTransactionManager extends JdbcTransactionManager {

    private static final ThreadLocal<Frame> FRAME_THREAD_LOCAL = new ThreadLocal<>();

    private final JSqlClient sqlClient;

    public JimmerTransactionManager(JSqlClient sqlClient) {
        super(dataSourceOf(sqlClient));
        this.sqlClient = sqlClient;
    }

    public JimmerTransactionManager(KSqlClient sqlClient) {
        this(sqlClient.getJavaClient());
    }

    @Deprecated
    @Override
    public final void setDatabaseProductName(String dbName) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public final void setDataSource(DataSource dataSource) {
        DataSource oldDataSource = getDataSource();
        if (oldDataSource == null) {
            super.setDataSource(dataSource);
        } else if (oldDataSource != dataSource) {
            throw new IllegalStateException("dataSource has already been set");
        }
    }

    @Override
    protected void doBegin(@NotNull Object transaction, @NotNull TransactionDefinition definition) {
        Frame frame = new Frame(sqlClient, FRAME_THREAD_LOCAL.get());
        FRAME_THREAD_LOCAL.set(frame);
        super.doBegin(transaction, definition);
    }

    @Override
    protected void doCleanupAfterCompletion(@NotNull Object transaction) {
        super.doCleanupAfterCompletion(transaction);
        Frame frame = FRAME_THREAD_LOCAL.get();
        Frame oldFrame = frame != null ? frame.parent : null;
        if (oldFrame == null) {
            FRAME_THREAD_LOCAL.remove();
        } else {
            FRAME_THREAD_LOCAL.set(oldFrame);
        }
    }

    public static JSqlClient sqlClient() {
        Frame frame = FRAME_THREAD_LOCAL.get();
        return frame != null ? frame.sqlClient : null;
    }

    private static DataSource dataSourceOf(JSqlClient sqlClient) {
        Objects.requireNonNull(sqlClient, "sqlClient cannot be null");
        if (sqlClient instanceof JTransactionalSqlClient) {
            throw new IllegalArgumentException(
                    "JimmerTransactionManager does not accept \"" +
                            JTransactionalSqlClient.class.getName() +
                            "\""
            );
        }
        ConnectionManager connectionManager = ((JSqlClientImplementor)sqlClient).getConnectionManager();
        if (!(connectionManager instanceof DataSourceAwareConnectionManager)) {
            throw new IllegalArgumentException(
                    "The data source of sql client must be an instance of \"" +
                            DataSourceAwareConnectionManager.class.getName() +
                            "\""
            );
        }
        return ((DataSourceAwareConnectionManager)connectionManager).getDataSource();
    }

    private static class Frame {

        final JSqlClient sqlClient;

        final Frame parent;

        private Frame(JSqlClient sqlClient, Frame parent) {
            this.sqlClient = sqlClient;
            this.parent = parent;
        }
    }
}
