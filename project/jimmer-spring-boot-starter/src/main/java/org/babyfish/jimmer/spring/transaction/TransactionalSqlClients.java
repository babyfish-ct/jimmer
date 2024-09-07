package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.kt.di.AbstractKSqlClientDelegate;
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

/**
 * This class should only be used if multiple data sources are used.
 */
public class TransactionalSqlClients {

    private static final JSqlClient JAVA_INSTANCE =
            new JTransactionalSqlClient();

    private TransactionalSqlClients() {}

    public static JSqlClient java() {
        return JAVA_INSTANCE;
    }

    public static KSqlClient kotlin() {
        return KotlinInstanceHolder.INSTANCE;
    }

    private static class KotlinInstanceHolder {

        static final KSqlClient INSTANCE = new AbstractKSqlClientDelegate() {

            private KSqlClientImplementor sqlClient;

            @NotNull
            protected KSqlClientImplementor sqlClient() {
                KSqlClientImplementor sqlClient = this.sqlClient;
                if (sqlClient == null) {
                    this.sqlClient = sqlClient = (KSqlClientImplementor) KSqlClientKt.toKSqlClient(JAVA_INSTANCE);
                }
                return sqlClient;
            }
        };
    }
}
