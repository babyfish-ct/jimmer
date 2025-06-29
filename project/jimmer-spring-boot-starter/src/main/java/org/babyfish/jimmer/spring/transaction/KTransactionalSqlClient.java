package org.babyfish.jimmer.spring.transaction;

import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.kt.di.AbstractKSqlClientDelegate;
import org.babyfish.jimmer.sql.kt.impl.KSqlClientImplementor;
import org.jetbrains.annotations.NotNull;

class KTransactionalSqlClient extends AbstractKSqlClientDelegate implements KSqlClientImplementor {

    private KSqlClientImplementor sqlClient;

    @NotNull
    protected KSqlClientImplementor sqlClient() {
        KSqlClientImplementor sqlClient = this.sqlClient;
        if (sqlClient == null) {
            this.sqlClient = sqlClient = (KSqlClientImplementor) KSqlClientKt.toKSqlClient(TransactionalSqlClients.java());
        }
        return sqlClient;
    }
}
