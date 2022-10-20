package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.event.EntityEvent
import java.util.*

interface KFilter<E: Any> {

    fun filter(args: KFilterArgs<E>)

    interface Parameterized<E: Any> : KFilter<E> {

        fun getParameters(): SortedMap<String, Any>?

        fun isAffectedBy(e: EntityEvent<*>): Boolean
    }
}