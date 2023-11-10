package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface OneToMany {

    /**
     * The property that owns the association.
     *
     * @return A property name of associated entity.
     */
    String mappedBy();

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
