package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.event.EntityEvent
import java.util.*

interface KCacheableFilter<E: Any> : KFilter<E> {

    val parameters: NavigableMap<String, Any>

    fun isAffectedBy(e: EntityEvent<*>): Boolean
}