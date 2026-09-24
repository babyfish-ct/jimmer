package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.kt.di.AbstractKSqlClientDelegate;
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class KTransactionalSqlClient extends AbstractKSqlClientDelegate implements KSqlClientImplementor {

    private final KSqlClientImplementor noTxSqlClient =
            (KSqlClientImplementor) KSqlClientKt.toKSqlClient(TransactionalSqlClients.java());

    private final Map<JSqlClientImplementor, KSqlClientImplementor> txSqlClients =
            new ConcurrentHashMap<>();

    @NotNull
    @Override
    protected KSqlClientImplementor sqlClient() {
        JSqlClient current = JimmerTransactionManager.sqlClient();
        if (current == null) {
            return noTxSqlClient;
        }
        JSqlClientImplementor actual = (JSqlClientImplementor) current;
        return txSqlClients.computeIfAbsent(
                actual,
                client -> (KSqlClientImplementor) KSqlClientKt.toKSqlClient(client)
        );
    }
}
