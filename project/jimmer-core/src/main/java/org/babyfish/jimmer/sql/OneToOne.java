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
}
