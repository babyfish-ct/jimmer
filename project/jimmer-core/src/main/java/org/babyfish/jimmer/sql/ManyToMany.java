package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface ManyToMany {

    /**
     * The property that owns the association. Required unless the relationship is unidirectional.
     *
     * <p>Once `mappedBy` is specified, the current property is just a mirror of the property in
     * the opposite entity that truly owns this association. Do not use `@JoinColumn` or `@JoinTable`
     * for association mapping.</p>
     *
     * <p>Unlike JPA, for bidirectional associations, Jimmer allows developers to arbitrarily decide
     * which side to be `mappedBy` side, which does not affect save behaviors.</p>
     *
     * @return A property name of associated entity.
     */
    String mappedBy() default "";

    /**
     * This configuration is used by object fetcher.
     *
     * <p>Object fetcher supports property-level filter so that the order by associated objects can be dynamically
     * controlled. However, fetching association property with property-level filter ignores the association cache</p>
     *
     * <p>In order to resolve this problem, you can specify the default order for associated objects</p>
     *
     * @return The ordered properties of associated objects
     */
    OrderedProp[] orderedProps() default {};

    /**
     * Additional cache invalidation configuration for this association, it is generally not needed.
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
    Invalidator invalidator() default @Invalidator;
}
