package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClient;

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

        static final KSqlClient INSTANCE = new KTransactionalSqlClient();
    }
}
