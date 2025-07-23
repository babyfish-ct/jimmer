package org.babyfish.jimmer.spring.transaction;

import kotlin.ExtensionFunctionType;
import kotlin.jvm.functions.Function1;
import kotlin.reflect.KClass;
import org.babyfish.jimmer.sql.kt.KSqlClientKt;
import org.babyfish.jimmer.sql.kt.ast.query.KConfigurableBaseQuery;
import org.babyfish.jimmer.sql.kt.ast.query.KMutableBaseQuery;
import org.babyfish.jimmer.sql.kt.ast.table.KBaseTable;
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
