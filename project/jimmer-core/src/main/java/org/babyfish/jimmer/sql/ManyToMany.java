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
}
