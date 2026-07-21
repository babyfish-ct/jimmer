package org.babyfish.jimmer.sql.di;

import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.runtime.*;

public abstract class JLazyInitializationSqlClient extends AbstractJSqlClientDelegate {

    private volatile JSqlClientImplementor sqlClient;

    protected final JSqlClientImplementor sqlClient() {
        JSqlClientImplementor sqlClient = this.sqlClient;
        if (sqlClient == null) {
            sqlClient = initialize();
        }
        return sqlClient;
    }

    private synchronized JSqlClientImplementor initialize() {
        JSqlClientImplementor sqlClient = this.sqlClient;
        if (sqlClient == null) {
            JSqlClient.Builder builder = createBuilder();
            sqlClient = (JSqlClientImplementor) builder.build();
            afterCreate(sqlClient);
            this.sqlClient = sqlClient;
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
