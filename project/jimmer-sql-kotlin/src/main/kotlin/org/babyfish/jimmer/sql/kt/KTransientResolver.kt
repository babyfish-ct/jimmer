package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.sql.TransientResolver
import java.sql.Connection
import java.util.*

interface KTransientResolver<ID: Any, V> : TransientResolver<ID, V> {

    override fun resolve(ids: Collection<ID>, con: Connection): Map<ID, V>

    interface Parameterized<ID: Any, V> : KTransientResolver<ID, V>, TransientResolver.Parameterized<ID, V>
}