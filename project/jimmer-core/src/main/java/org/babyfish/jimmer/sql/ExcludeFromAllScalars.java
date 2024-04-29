package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a scalar property is decorated by this annotation,
 * it will be automatically excluded from either
 * `allScalarsFields` of object fetcher or `#allScalars`
 * or DTO language. It can only be fetched by explicit
 * fetching, like the logical deleted field.
 *
 * <p>Please do not abuse this annotation. In fact,
 * there are not many properties suitable for using
 * this annotation, generally it is for password
 * properties or LOB properties.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
public @interface ExcludeFromAllScalars {}
