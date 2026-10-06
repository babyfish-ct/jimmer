package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the property that determines whether an entity is logically deleted.
 *
 * <p>Save commands preserve explicitly loaded values that represent a live entity.
 * For example, with enum values {@code NEW}, {@code RUNNING}, and {@code DELETED},
 * {@code @LogicalDeleted("DELETED")} permits saving either {@code NEW} or {@code RUNNING}.
 * An unloaded property is initialized only for insertion; it is not reset during an update,
 * including the update branch of an upsert. {@link Default} can specify the initial live value.</p>
 *
 * <p>Save commands reject loaded values that represent deletion, including values supplied by
 * draft callbacks. Use a delete command to apply logical deletion and its association rules.</p>
 *
 * <p>When matching by a business key, non-null enum and integer flags participate in the
 * match with their exact live value. An unloaded flag uses its initial value for matching.
 * Use the entity ID to change such a flag on an existing row.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface LogicalDeleted {

    /**
     * @return A value indicating that the current entity is logical deleted, it can be
     * <ul>
     *     <li>true</li>
     *     <li>false</li>
     *     <li>integer, such as 0, 1, 2, 3</li>
     *     <li>Constant name of the enum returned by the current decorated property</li>
     *     <li>null</li>
     *     <li>now</li>
     * </ul>
     *
     * <p>For long and uuid, `generatorType` or `generatorRef` must be specified</p>
     */
    String value() default "";

    Class<? extends LogicalDeletedValueGenerator<?>> generatorType() default LogicalDeletedValueGenerator.None.class;

    String generatorRef() default "";
}
