package org.babyfish.jimmer.sql;

/**
 * The property-level cache invalidation configuration, if the property is association, it is generally not needed.
 *
 * <p>When the associations in the database change, such as when a foreign key is modified, records in a middle table
 * are deleted or inserted, Jimmer will automatically invalidate the affected association caches.</p>
 *
 * Therefore
 *
 * <ul>
 *     <li>When no filters are applied to the associated objects (the association cache is single-viewed),
 *     do not use this configuration.</li>
 *
 *     <li>Even in multi-view cache scenarios, do not check if the associations
 *     in the database have changed in the implementation of the corresponding
 *     implementation of `org.babyfish.jimmer.sql.cache.PropCacheInvalidator`,
 *     because this is always done automatically.</li>
 * </ul>
 *
 * <p>If the fields that filter care about are in the table of the associated objects (e.g. in multi-tenant
 * scenarios, the associated objects often directly store their tenant information), then this configuration
 * is not needed - just override `CacheableFilter.isAffectedBy(EntityEvent)` directly.</p>
 *
 * <p>Please only use this configuration when the fields that filter care about are not in the table
 * of the associated objects.</p>
 */
public @interface Invalidator {

    String ref() default "";

    Class<?> value() default void.class;
}
