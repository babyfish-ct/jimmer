package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Be different with {@link JoinTable}, this annotation
 * is used by UNSTRUCTURED many-to-many mappings.
 *
 * For example:
 * <pre>{@code
 * @ManyToMany
 * @JoinSql("SOME_FUN_1(%alias.FIELD_X) = SOME_FUN_2(%target_alias.FIELD_Y)")
 * List<Target> targets();
 * }</pre>
 *
 * Applicable restrictions
 * <ul>
 *     <li>Can only used with {@link ManyToMany}</li>
 *     <li>
 *         Cannot be used by remote association
 *         (The microservice names of declaring type and target type are different)
 *     </li>
 *     <li>Cannot be optimized by `half-join`</li>
 *     <li>Cannot be cached</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface JoinSql {

    String value();
}
