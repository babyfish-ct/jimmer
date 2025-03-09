package org.babyfish.jimmer.spring.cfg.support;

import org.babyfish.jimmer.sql.transaction.Propagation;
import org.babyfish.jimmer.sql.transaction.TxConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringConnectionManager implements DataSourceAwareConnectionManager, TxConnectionManager {

    private final DataSource dataSource;

    private final Supplier<DataSourceTransactionManager> transactionManagerResolver;

    private volatile Object transactionManagerOrException;

    public SpringConnectionManager(DataSource dataSource) {
        this.dataSource = dataSource;
        this.transactionManagerResolver = null;
    }

    public SpringConnectionManager(
            DataSource dataSource,
            Supplier<DataSourceTransactionManager> transactionManagerResolver
    ) {
        this.dataSource = dataSource;
        this.transactionManagerResolver = transactionManagerResolver;
    }

    @NotNull
    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public final <R> R execute(Function<Connection, R> block) {
        return execute(null, block);
    }

    @Override
    public final <R> R execute(@Nullable Connection con, Function<Connection, R> block) {
        if (con != null) return block.apply(con);

        Connection newConnection = DataSourceUtils.getConnection(dataSource);
        try {
            return block.apply(newConnection);
        } finally {
            DataSourceUtils.releaseConnection(newConnection, dataSource);
        }
    }

    @Override
    public final <R> R executeTransaction(Propagation propagation, Function<Connection, R> block) {
        DataSourceTransactionManager tm = transactionManager();
        TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition(behavior(propagation)));
        R result;
        try {
            result = execute(block);
        } catch (RuntimeException | Error ex) {
            tm.rollback(ts);
            throw ex;
        }
        tm.commit(ts);
        return result;
    }

    private DataSourceTransactionManager transactionManager() {
        Object obj = transactionManagerObject();
        if (obj instanceof RuntimeException) {
            throw (RuntimeException) obj;
        }
        if (obj instanceof Error) {
            throw (Error) obj;
        }
        return (DataSourceTransactionManager) obj;
    }

    private Object transactionManagerObject() {
        if (transactionManagerOrException == null) {
            synchronized (this) {
                if (transactionManagerOrException == null) {
                    if (transactionManagerResolver == null) {
                        transactionManagerOrException = new IllegalStateException(
                                "The current SpringConnectionManager does not support " +
                                        "transaction management because its transactionManagerResolver " +
                                        "is not set"
                        );
                    } else {
                        try {
                            transactionManagerOrException = transactionManagerResolver.get();
                        } catch (RuntimeException | Error ex) {
                            transactionManagerOrException = ex;
                        }
                        if (transactionManagerOrException == null) {
                            transactionManagerOrException = new IllegalStateException(
                                    "The current SpringConnectionManager does not support " +
                                            "transaction management its transactionManagerResolver " +
                                            "returns null"
                            );
                        }
                    }
                }
            }
        }
        return transactionManagerOrException;
    }

    private int behavior(Propagation propagation) {
        switch (propagation) {
            case REQUIRES_NEW:
                return TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            case SUPPORTS:
                return TransactionDefinition.PROPAGATION_SUPPORTS;
            case NOT_SUPPORTED:
                return TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
            case MANDATORY:
                return TransactionDefinition.PROPAGATION_MANDATORY;
            case NEVER:
                return TransactionDefinition.PROPAGATION_NEVER;
            default:
                // REQUIRED:
                return TransactionDefinition.PROPAGATION_REQUIRED;
        }
    }
}
