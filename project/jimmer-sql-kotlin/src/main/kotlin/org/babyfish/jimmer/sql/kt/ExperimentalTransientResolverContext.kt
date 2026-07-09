package org.babyfish.jimmer.sql.kt

/**
 * Transient resolver context is experimental and may change in future releases.
 *
 * @since 0.10.13
 */
@RequiresOptIn(message = "Transient resolver context is experimental and may change in future releases")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalTransientResolverContext
