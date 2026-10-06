package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.kt.di.AbstractKSqlClientDelegate;
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

class KTransactionalSqlClient extends AbstractKSqlClientDelegate implements KSqlClientImplementor {

    private final KSqlClientImplementor noTxSqlClient =
            (KSqlClientImplementor) KSqlClientKt.toKSqlClient(TransactionalSqlClients.java());

    @NotNull
    @Override
    protected KSqlClientImplementor sqlClient() {
        KSqlClientImplementor sqlClient = JimmerTransactionManager.kotlinSqlClient();
        return sqlClient != null ? sqlClient : noTxSqlClient;
    }

    @NotNull
    @Override
    public JSqlClientImplementor getJavaClient() {
        return (JSqlClientImplementor) TransactionalSqlClients.java();
    }
}
