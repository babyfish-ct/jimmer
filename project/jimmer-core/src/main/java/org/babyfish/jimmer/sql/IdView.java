package org.babyfish.jimmer.sql;

import kotlin.annotation.AnnotationTarget;

import java.lang.annotation.*;

/**
 * For example,
 * <pre>{@code
 * public interface Book {
 *
 *     @ManyToOne
 *     BookStore store();
 *
 *     @OneToMany
 *     List<Author> authors();
 *
 *     @IdView
 *     Long storeId();
 *
 *     @IdView("authors")
 *     List<Long> authorIds();
 * }}</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(allowedTargets = AnnotationTarget.PROPERTY)
@Target(ElementType.METHOD)
public @interface IdView {

    String value() default "";
}
