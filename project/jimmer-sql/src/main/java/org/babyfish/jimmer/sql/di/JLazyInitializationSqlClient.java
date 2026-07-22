package org.babyfish.jimmer.sql.di;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

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

    public JSqlClientImplementor unwrap() {
        return sqlClient();
    }

    public static JSqlClient.Builder newBuilder() {
        return JSqlClient.newBuilder();
    }
}
