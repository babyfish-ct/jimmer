package org.babyfish.jimmer.sql.kt.filter

import org.babyfish.jimmer.sql.cache.PropCacheInvalidator
import org.babyfish.jimmer.sql.event.EntityEvent
import java.util.*

interface KCacheableFilter<E: Any> : KFilter<E>, PropCacheInvalidator {

    /**
     * Return the sub keys of association cache for multi-view cache
     * @see <a href="https://babyfish-ct.github.io/jimmer-doc/docs/cache/multiview-cache/concept#subkey">SubKeys for multi-view cache</a>
     * @return The sub key map
     */
    fun getParameters(): SortedMap<String, Any>?

    /**
     * Is the association cache affected by the change of CURRENT table?
     * @param e The change event of CURRENT table
     * @return Whether the association cache should be affected
     */
    fun isAffectedBy(e: EntityEvent<*>): Boolean
}