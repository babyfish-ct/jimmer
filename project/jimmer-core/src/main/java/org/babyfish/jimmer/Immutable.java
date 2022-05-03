package org.babyfish.jimmer;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Immutable {

    /**
     * Default nullity of members of current annotated interface
     */
    Nullity value() default Nullity.NON_NULL;

    enum Nullity {
        NON_NULL,
        NULLABLE
    }
}
