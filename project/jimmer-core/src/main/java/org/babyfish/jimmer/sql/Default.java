package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to decorates scalar properties except `@Id`, `@Key` and `@Version`,
 * to specify default value for insert operation.
 *
 * <ul>
 *     <li>The default value only affects insert operation, update operation will never be affected
 *     (Unloaded properties still mean not updating)</li>
 *     <li>When insert an object, if unloaded property is not filled by `DraftInterceptor`,
 *     this default value will be used</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface Default {

    String value();
}
