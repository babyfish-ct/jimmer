package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.sql.meta.UserIdGenerator

class PreparedIdGenerator<T>(
    vararg ids: T
) : UserIdGenerator<T> {

    private val itr = ids.toList().iterator()

    override fun generate(entityType: Class<*>?): T =
        itr.next()
}