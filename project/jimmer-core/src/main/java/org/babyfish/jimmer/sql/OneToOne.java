package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface OneToOne {

    /**
     * The property that owns the association. Required unless the relationship is unidirectional.
     *
     * Here is the English translation:
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
     * This argument cannot be specified when `mappedBy` is specified!
     *
     * <p>Sometimes, the foreign key in the database is not null, but the developer has to
     * declare the many-to-one property as a nullable property in the entity type.</p>
     *
     * <p>Global filter is a typical scenario. Although the non-null foreign key in the
     * database indicates that the parent object exists absolutely, once the filtering
     * behavior is applied to the class to which the parent object belongs, it is still
     * possible that no parent object data can be found.</p>
     *
     * <p>At this time, in order to cope with the query business, we hope to set a
     * many-to-one association as nullable, but we still hope that the user must specify
     * a non-null parent object in save business. At this time, you can specify the
     * `inputNotNull` of this annotation.</p>
     */
    boolean inputNotNull() default false;

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
