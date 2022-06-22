package org.babyfish.jimmer;

import java.lang.annotation.*;

/**
 * Specifies that an interface is an immutable interface.
 *
 * If a top-level user interface is decorated with
 * this annotation or javax.persistence.Entity,
 * the Annotation Processor will generate more source code for it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Immutable {

    /**
     * <p>
     *     Default nullity of members of current annotated interface.
     * </p>
     *
     * <p>
     *     The immutable interface can define multiple properties,
     *     and jimmer will try its best to determine whether each property is nullable.
     * </p>
     *
     * <p>
     *     If it cannot determine whether a property is nullable,
     *     it will refer to this value.
     * </p>
     */
    Nullity value() default Nullity.NON_NULL;

    enum Nullity {
        NON_NULL,
        NULLABLE
    }
}
