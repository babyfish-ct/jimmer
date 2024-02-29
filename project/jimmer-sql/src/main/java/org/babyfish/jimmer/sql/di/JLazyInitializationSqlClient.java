package org.babyfish.jimmer.sql.di;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class JLazyInitializationSqlClient extends AbstractJSqlClientDelegate {

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private JSqlClientImplementor sqlClient;

    protected final JSqlClientImplementor sqlClient() {
        Lock lock;
        JSqlClientImplementor sqlClient;

        (lock = readWriteLock.readLock()).lock();
        try {
            sqlClient = this.sqlClient;
        } finally {
            lock.unlock();
        }

        if (sqlClient == null) {
            (lock = readWriteLock.writeLock()).lock();
            try {
                sqlClient = this.sqlClient;
                if (sqlClient == null) {
                    JSqlClient.Builder builder = createBuilder();
                    builder.setInitializationType(InitializationType.MANUAL);
                    sqlClient = (JSqlClientImplementor) builder.build();
                    afterCreate(sqlClient);
                    this.sqlClient = sqlClient;
                }
            } finally {
                lock.unlock();
            }
        }

        return sqlClient;
    }

    protected abstract JSqlClient.Builder createBuilder();

    protected void afterCreate(JSqlClientImplementor sqlClient) {}

    @SuppressWarnings("unchecked")
    @Override
    public <T extends SqlContext> T unwrap() {
        return (T) sqlClient();
    }

    public static JSqlClient.Builder newBuilder() {
        return JSqlClient.newBuilder();
    }
}
