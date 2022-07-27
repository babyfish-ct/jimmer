package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.sql.meta.UserIdGenerator

class PreparedIdGenerator(
    vararg ids: Any
) : UserIdGenerator {

    private val itr = ids.toList().iterator()

    override fun generate(entityType: Class<*>?): Any =
        itr.next()
}