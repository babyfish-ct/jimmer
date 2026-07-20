package org.babyfish.jimmer.sql.kt.transaction

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.transaction.Propagation
import org.babyfish.jimmer.sql.transaction.TargetAnnotation
import org.babyfish.jimmer.sql.transaction.Tx

@TargetAnnotation(Component::class)
@Tx(Propagation.MANDATORY)
open class ServiceA(
    protected val sqlClient: KSqlClient
) {

    open fun a() {}

    @Tx(Propagation.REQUIRES_NEW)
    internal open fun b() {}
}