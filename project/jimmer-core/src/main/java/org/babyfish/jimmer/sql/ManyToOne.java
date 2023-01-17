package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface ManyToOne {

    /**
     * Sometimes, the foreign key in the database is not null, but the developer has to
     * declare the many-to-one property as a nullable property in the entity type.
     *
     * Global filter is a typical scenario. Although the non-null foreign key in the
     * database indicates that the parent object exists absolutely, once the filtering
     * behavior is applied to the class to which the parent object belongs, it is still
     * possible that no parent object data can be found.
     *
     * At this time, in order to cope with the query business, we hope to set a
     * many-to-one association as nullable, but we still hope that the user must specify
     * a non-null parent object in save business. At this time, you can specify the
     * `inputNotNull` of this annotation.
     */
    boolean inputNotNull() default false;
}
