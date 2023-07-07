package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.cache.CacheOperator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public abstract class AbstractCacheOperator implements CacheOperator {

    private JSqlClientImplementor sqlClient;

    @Override
    public final void initialize(JSqlClient sqlClient) {
        if (sqlClient == null) {
            throw new IllegalArgumentException("The argument `sqlClient` cannot be null");
        }
        if (this.sqlClient != null && this.sqlClient != sqlClient) {
            throw new IllegalStateException(
                    "The current cache operator has already be initialized, " +
                            "it cannot be shared by multiple sql clients"
            );
        }
        JSqlClientImplementor implementor = (JSqlClientImplementor) sqlClient;
        onInitialize(implementor);
        this.sqlClient = implementor;
    }

    protected final JSqlClientImplementor sqlClient() {
        JSqlClientImplementor implementor = sqlClient;
        if (implementor == null) {
            throw new IllegalStateException("The current cache operator has not been initialized");
        }
        return implementor;
    }

    protected void onInitialize(JSqlClientImplementor sqlClient) {}
}
