package org.babyfish.jimmer.sql.kt.filter.common

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.sql.event.AssociationEvent
import org.babyfish.jimmer.sql.event.EntityEvent
import org.babyfish.jimmer.sql.kt.filter.KCacheableFilter
import org.babyfish.jimmer.sql.kt.model.filter.File
import java.util.*

class CacheableFileFilter : FileFilter(), KCacheableFilter<File> {

    override fun getParameters(): SortedMap<String, Any> =
        sortedMapOf("userId" to currentUserId)

    override fun isAffectedBy(e: EntityEvent<*>): Boolean = false

    override fun getAffectedSourceIds(e: AssociationEvent): Collection<*>? {
        if (e.immutableProp == File::users.toImmutableProp()) {
            return listOf(e.sourceId)
        }
        return null
    }
}