package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * In ORM, there are two concepts: Surrogate id and Natural id.
 *
 * <ul>
 *     <li>{@link Id} is used to specify surrogate id</li>
 *     <li>This annotation is used to specify natural id</li>
 * </ul>
 *
 * <b>Notes</b>
 * <ol>
 *     <li>
 *         Multiple properties based on column(s) of an entity
 *         can be decorated by this annotation
 *     </li>
 *     <li>
 *         One-to-one/Many-to-one association property
 *         based on foreign key can be a part of key
 *     </li>
 * </ol>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Key {}
