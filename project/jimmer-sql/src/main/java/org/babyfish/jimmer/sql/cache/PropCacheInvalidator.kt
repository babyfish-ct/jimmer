package org.babyfish.jimmer.sql.cache

import org.babyfish.jimmer.sql.event.AssociationEvent
import org.babyfish.jimmer.sql.event.EntityEvent

interface PropCacheInvalidator {

    fun getAffectedSourceIds(e: EntityEvent<*>): Collection<Any?>?

    fun getAffectedSourceIds(e: AssociationEvent): Collection<Any?>?
}
