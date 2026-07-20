package org.babyfish.jimmer.sql.kt.transaction

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.transaction.Propagation
import org.babyfish.jimmer.sql.transaction.Tx

open class ServiceB(
    protected val sqlClient: KSqlClient
) {
    @Tx
    open fun a() {}

    @Tx(Propagation.REQUIRES_NEW)
    protected open fun b() {}
}