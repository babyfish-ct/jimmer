package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * This annotation decorate many-to-one association.
 *
 * When any of the following occurs, the associated property must be nullable:
 * <ul>
 *  <li>The foreign key in the database is null</li>
 *  <li>
 *      The foreign key is fake, that means it is not a real foreign key constraint in the database,
 *      but rather a convention that exists only in the developer's subjective consciousness.
 *      (In this case, it is necessary to use @JoinColumn(name = "...", foreignKey = false))
 *  </li>
 *  <li>The current association is based on middle table, not on foreign key</li>
 *  <li>
 *      The current association is a remote association, that means
 *      the microservice names of the declaring entity and target entity are different
 *  </li>
 *  <li>
 *      There are some global filters whose generic type is the target entity type,
 *      even if all the global filters
 *  </li>
 * </ul>
 */
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
