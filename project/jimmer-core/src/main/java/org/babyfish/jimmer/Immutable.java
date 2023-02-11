package org.babyfish.jimmer;

import java.lang.annotation.*;

/**
 * Specifies that an interface is an immutable interface.
 *
 * If a top-level user interface is decorated with
 * this annotation or org.babyfish.jimmer.sql.Entity,
 * the Annotation Processor will generate more source code for it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Immutable {}
