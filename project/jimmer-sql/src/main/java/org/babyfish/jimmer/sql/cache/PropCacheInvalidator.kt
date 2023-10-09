package org.babyfish.jimmer.sql.cache

import org.babyfish.jimmer.sql.event.AssociationEvent
import org.babyfish.jimmer.sql.event.EntityEvent


/**
 * The property-level cache invalidation configuration.
 *
 *
 * Don't implement this interface directly, it has already been inherited by [Ca]
 *
 *
 * When the associations in the database change, such as when a foreign key is modified, records in a middle table
 * are deleted or inserted, Jimmer will automatically invalidate the affected association caches.
 *
 * Therefore
 *
 *
 *  * When no filters are applied to the associated objects (the association cache is single-viewed),
 * do not override the methods of this interface.
 *
 *  * Even in multi-view cache scenarios, do not check if the associations in the database have changed
 * in the implementation class, because this is always done automatically.
 *
 *
 *
 * If the fields that filter care about are in the table of the associated objects (e.g. in multi-tenant
 * scenarios, the associated objects often directly store their tenant information), then this configuration
 * is not needed - just override `CacheableFilter.isAffectedBy(EntityEvent)` directly.
 *
 *
 * Please only override the methods of this interface when the fields that filter care about are
 * not in the table of the associated objects.
 */
interface PropCacheInvalidator {

    fun getAffectedSourceIds(e: EntityEvent<*>): Collection<*>? = null

    fun getAffectedSourceIds(e: AssociationEvent): Collection<*>? = null
}
